/*
 ****************************************************************************************
 * Program        : PULSE.CPP                                                           *
 * Description    : Demo program for pulse output function                              *
 * Boards Supp.   :                                                                     *
 * APIs used      : DRV_DeviceOpen,DRV_DeviceClose, DRV_GetErrorMessage,                *
 *                  DRV_CounterReset, DRV_CounterPulseStart                             *
 * Revision       : 1.00                                                                *
 * Date           : 7/8/1999                   Advantech Co., Ltd.                      *
 ****************************************************************************************
 *
 * Modificado para Remote Laboratory Framework v0.1
 *
 * by Carlos A. Rodríguez Mecha
 *
 */
#include <windows.h>
#include <windef.h>
#include <stdio.h>
#include <conio.h>
#include <iostream>
#include <malloc.h> 
#include <process.h>

#define WIN_CONSOLE
#define TOOLKEY "ebb8b1dbec1a998a061b65b16017cab6896b630e"
#include "C:\Program Files\Advantech\Adsapi\Include\driver.h"

using namespace std; 
using namespace System;
using namespace System::IO;
using namespace LibTool;
using namespace System::Runtime::InteropServices;

using namespace std; 
using namespace System;
using namespace System::IO;
using namespace LibTool;
using namespace System::Runtime::InteropServices;
using namespace System::Threading;
using namespace System::Globalization;

int end = 0;
int error = 0;
ULONG  lDevNum;
long   lDriverHandle;
USHORT usChan;
float  fOutValue;


/******************************
 * Local function declaration *
 ******************************/
void ErrorHandler(DWORD dwErrCde);
void ErrorStop(long*, DWORD, RLF_Manager ^);
BOOL AllocateDataBuffer(long, int, int, USHORT**, void**);
void FreeDataBuffer(USHORT* pusINTBuf, void *pUserBuf);

/**
 * Limpia las salidas analógicas del dispositivo.
 */
void ClearPulse(){

	long lDriverHandle;
	PT_AOVoltageOut tAOVoltageOut;
	DWORD  dwErrCde;

	// 1. Apertura del dispositivo.
	dwErrCde = DRV_DeviceOpen(lDevNum, &lDriverHandle);   
    if (dwErrCde != SUCCESS)
    {
        return;
    }

	// 2. Envío de 0 voltios.
	tAOVoltageOut.chan = usChan;
	tAOVoltageOut.OutputValue = 0;
	dwErrCde = DRV_AOVoltageOut(lDriverHandle, &tAOVoltageOut);

	// 3. Cierre.
	dwErrCde = DRV_DeviceClose(&lDriverHandle);

	return;
}

/**
 * Ejecución del hilo principal de lectura.
 */
void readBoard(void * args)
{
	DWORD  dwErrCde;
    int    i, iSamples = 500;
	USHORT usGain = 1;

    USHORT *pusINTBuf;      //INT internal transfer buffer
    float  *pfUserBuf;      //User buffer for retrieve data
    USHORT usActiveBuf;     //for PT_FAICheck to return data
    USHORT usStopped;
    ULONG  ulRetrieved;
    USHORT usOverrun;
    USHORT usHalfReady;
    ULONG  ulPreRetrieved;
    USHORT usFifoSize = 1;		//Fifo size

    PT_FAIIntStart  tFAIIntStart;
    PT_FAICheck     tFAICheck;
    PT_FAITransfer  tFAITransfer;

	// 1. Apertura del fichero.
	String ^ fileName = "" + DateTime::Now.Ticks + ".csv";
	Console::WriteLine("Los resultados se podrán obtener mediante el fichero {0}.", fileName); 
	StreamWriter^ sw = gcnew StreamWriter("data\\" + fileName);

	// 2. Configuración de las muestras.
	if( AllocateDataBuffer(
            lDriverHandle,              // driver handle
            iSamples,                   // data count
            sizeof(float),              // size of one data
            &pusINTBuf,                 // INT buffer allocated
            (void**)&pfUserBuf)==false) // user buffer allocated
    {
        Console::WriteLine("Error en el buffer.");
		sw->Close();
		error = 1;
		return;
    }
	
	// 2.1 Configuración de la interrupción.
    tFAIIntStart.TrigSrc = 0;          // 0: internal trigger, 1: external trigger
    tFAIIntStart.SampleRate = 100;    // pacer rate: 10KHz
    tFAIIntStart.chan   = usChan;      // input channel
    tFAIIntStart.gain   = usGain;           // gain code:0, check manual for detail input range
    tFAIIntStart.count  = iSamples;    // number of samples 
    tFAIIntStart.buffer = pusINTBuf;   // data buffer pointer
    tFAIIntStart.cyclic = 0;           // 0: non-cyclic mode, 1: cyclic-mode
    tFAIIntStart.IntrCount = usFifoSize;      //  FIFO interrupt

	dwErrCde = DRV_FAIIntStart(lDriverHandle, &tFAIIntStart);
    if (dwErrCde != SUCCESS)
    {
        Console::WriteLine("Error en el buffer.");
		sw->Close();
		error = 1;
		return;
    }

	// 3. Lectura de las muestras.
    tFAICheck.ActiveBuf = &usActiveBuf; // not used for single buffer
    tFAICheck.stopped   = &usStopped;   // returned status: 1: complete, 0: imcomplete
    tFAICheck.retrieved = &ulRetrieved; // actual conversion count
    tFAICheck.overrun   = &usOverrun;   // not used for non-cyclic mode
    tFAICheck.HalfReady = &usHalfReady; // not used for single buffer
    ulPreRetrieved = 0;
    do
    {
        dwErrCde = DRV_FAICheck(lDriverHandle, &tFAICheck);
        if (ulPreRetrieved > ulRetrieved) break;

        ulPreRetrieved = ulRetrieved;
    } while ((dwErrCde==0) && (usStopped != 1));

	if (dwErrCde != SUCCESS)
    {
		DRV_FAIStop(lDriverHandle);
		Console::WriteLine("Error en el buffer.");
		sw->Close();
		error = 1;
		return;
    }

	// Step 7: Stop A/D conversion for high speed
    dwErrCde = DRV_FAIStop(lDriverHandle);
    if (dwErrCde != SUCCESS)
    {
		Console::WriteLine("Error en el dispositivo.");
		sw->Close();
		error = 1;
		return;
    }

    // 4. Obtener los datos
    tFAITransfer.ActiveBuf = 0;         // 0: single buffer, 1: double buffer
    tFAITransfer.DataType = 1;          // 0: raw or unsigned short data, 1: float data
    tFAITransfer.start    = 0;          // 0: returned starting point of the data buffer
    tFAITransfer.count    = iSamples;   // requested data length. It requests all data.
    tFAITransfer.overrun  = &usOverrun; // returned overrun flag
    tFAITransfer.DataBuffer = pfUserBuf;

    dwErrCde = DRV_FAITransfer(lDriverHandle, &tFAITransfer);
    if (dwErrCde != SUCCESS)
    {
		Console::WriteLine("Error en el dispositivo.");
		sw->Close();
		error = 1;
        return;
    }
	
	CultureInfo ^ ci = gcnew CultureInfo("en-US");

	// 5. Escritura en el fichero.
	System::Double ref = pfUserBuf[0];
    for (i = 0; i < iSamples; i ++)
    {
		System::Double t = ((float) i)/100.00;
		System::Double b = pfUserBuf[i];
		System::Double v = (i >= 25) ? fOutValue-(float) 5 : ref;
		sw->WriteLine("{0},{1},{2}", t.ToString("F", ci), b.ToString("F4", ci), v.ToString("F", ci));
    }

	// 6. Cierre del fichero.
	sw->Close();

	// 7. Free buffer.
	FreeDataBuffer(pusINTBuf, (void *)pfUserBuf);

	return;
}

int main(array<System::String ^> ^args)
{
    DWORD  dwErrCde;
	int end = 0;
	
	char option;

    StreamWriter ^ stream = gcnew StreamWriter(Console::OpenStandardOutput());
	stream->AutoFlush = true;
	Console::SetOut(stream);

	// 1. Inicio RLF
	RLF_Manager ^ manager = gcnew RLF_Manager();
	
	try {
		manager->init(TOOLKEY);
	} catch (RLF_Exception ^ e){
		Console::WriteLine("Error con la base de datos al iniciar. " + e->getMsg());
		return 1;
	}

	// 2. Número de tarjeta.
	try {
		lDevNum = System::UInt64::Parse(manager->getConst("device")->getValue());
	} catch (RLF_Exception ^ e){
		Console::WriteLine("Error con la base de datos al obtener datos." + e->getMsg());
		try {
			manager->finalize(1, "Error.");
		} catch (...){
		}
		return 1;
	}
	
	// 3. Canal.
	try {
		usChan = System::UInt16::Parse(manager->getConst("channel")->getValue());
	} catch (RLF_Exception ^ e){
		Console::WriteLine("Error con la base de datos al obtener datos." + e->getMsg());
		try {
			manager->finalize(1, "Error.");
		} catch (...){
		}
		return 1;
	} catch (...){
		try {
			manager->throwException("FORMAT", "Formato de parámetro channel inválido.");
			manager->finalize(1, "Error.");
		} catch (...){
		}
		return 1;
	}

	if (args->Length>=1 && args[0]->CompareTo("--clear") == 0){
		ClearPulse();
		try {
			manager->finalize(0, "Correcto.");
		} catch (...){
		}
		return 1;
	}

	// 4. Voltaje
	try {
		fOutValue = System::Double::Parse(manager->getParameter("volts")->getValue());
	} catch (RLF_Exception ^ e){
		Console::WriteLine("Error con la base de datos al obtener datos." + e->getMsg());
		try {
			manager->finalize(1, "Error.");
		} catch (...){
		}
		return 1;
	} catch (...){
		try {
			manager->throwException("FORMAT", "Formato de parámetro frequency inválido.");
			manager->finalize(1, "Error.");
		} catch (...){
		}
		return 1;
	}
	
    //Step 3: Open device
    dwErrCde = DRV_DeviceOpen(lDevNum, &lDriverHandle);   
    if (dwErrCde != SUCCESS)
    {
        ErrorHandler(dwErrCde);
        try {
			manager->finalize(1, "Error.");
		} catch (...){
		}
        return 1;
    }
	
	PT_AOVoltageOut tAOVoltageOut;
	tAOVoltageOut.chan = usChan;
    tAOVoltageOut.OutputValue = fOutValue;
	
	Console::WriteLine("Introduzca 'p' si quiere enviar un pulso durante 5 segundos. 'e' para salir.");
	cin >> option;

	if (option == 'p'){
		
		_beginthread(readBoard, 0, 0);

		Thread::Sleep(250);
		Console::WriteLine("Enviando...");
		dwErrCde = DRV_AOVoltageOut(lDriverHandle, &tAOVoltageOut);
		if (dwErrCde != SUCCESS)
		{
			ErrorStop(&lDriverHandle, dwErrCde, manager);
			try {
				manager->finalize(1, "Error.");
			} catch (...){
			}
			return 1;
		}
		
		Thread::Sleep(5 * 1000);
		Console::WriteLine("Finalizado.");

		PT_AOVoltageOut tAOVoltageOut2;
		tAOVoltageOut2.chan = usChan;
		tAOVoltageOut2.OutputValue = 0;
		dwErrCde = DRV_AOVoltageOut(lDriverHandle, &tAOVoltageOut2);
		if (dwErrCde != SUCCESS)
		{
			ErrorStop(&lDriverHandle, dwErrCde, manager);
			try {
				manager->finalize(1, "Error.");
			} catch (...){
			}
			return 1;
		}
		
	}

	dwErrCde = DRV_DeviceClose(&lDriverHandle);
	
	try {
		manager->finalize(0, "Correcto.");
	} catch (...){
	}
	return 0;

}//main

/**********************************************************************
 * Function: ErrorHandler
 *           Show the error message for the corresponding error code
 * input:    dwErrCde, IN, Error code
 * return:   none
 **********************************************************************/
void ErrorHandler(DWORD dwErrCde)
{
    char szErrMsg[180];

    DRV_GetErrorMessage(dwErrCde, szErrMsg);
    String ^ errorMsg = gcnew String(szErrMsg);
	Console::Write("\nError({0:G}): {1}\n", dwErrCde & 0xffff, errorMsg);
}//ErrorHandler

/**********************************************************************
 * Function:   ErrorStop
 *             Release all resource and terminate program if error occurs 
 * Paramaters: pDrvHandle, IN/OUT, pointer to Driver handle
 *             dwErrCde, IN, Error code.
 *			   manager, IN, Gestor de la herramienta.
 * return:     none             
 **********************************************************************/
void ErrorStop(long *pDrvHandle, DWORD dwErrCde, RLF_Manager ^ manager)
{
    //Error message 
    ErrorHandler(dwErrCde);
    Console::Write("Programa terminado con errores!\n");
    
    //Close device
    DRV_DeviceClose(pDrvHandle);

    try {
		manager->finalize(1, "Error.");
	} catch (...){
	}
    exit(1);
}//ErrorStop

/**********************************************************************
 * Function:   AllocateDataBuffer
 *             Allocate data buffer for INT transfer.
 * Paramaters: lDrvHandle, IN, Driver handle
 *             iSamples, IN, Data count.
 *             iDataSize, IN, Size of one data
 *             plINTBuf, OUT, Interrupt buffer pointer.
 *             pUserBuf, OUT, user data buffer. Converted data is stored
 *                       here.
 * return:     TRUE - memory allocate successfully
 *             FALSE - allocate failed
 **********************************************************************/
BOOL AllocateDataBuffer(
        long  lDrvHandle,
        int   iSamples,
        int   iDataSize,
        USHORT ** pusINTBuf,
        void   ** pUserBuf)
{

    // Allocate INT buffer for driver
    *pusINTBuf = (USHORT*) GlobalAlloc(GPTR, iSamples * sizeof(USHORT));
    if (pusINTBuf == NULL)
    {
        Console::Write("\nError: Allocate memory error.\n");
        return(false);
    }

    // Allocate memory for user buffer.
    *pUserBuf = malloc(iSamples * iDataSize);
    if (pUserBuf == NULL)
    {
        free(*pusINTBuf);
        Console::Write("\nError: Allocate memory error.\n");
        return(false);
    }

    return(true);
}//AllocateDataBuffer

/**********************************************************************
 * Function:   FreeDataBuffer
 *             Free data buffer allocated by function AllocateDataBuffer
 * Paramaters: pusINTBuf, IN, Address of Interrupt buffer
 *             pUserBuf, IN, Address of user buffer
 * return:     none
 **********************************************************************/
void FreeDataBuffer(USHORT* pusINTBuf, void* pUserBuf)
{
    GlobalFree((HGLOBAL)pusINTBuf);
    free(pUserBuf);
}//FreeDataBuffer