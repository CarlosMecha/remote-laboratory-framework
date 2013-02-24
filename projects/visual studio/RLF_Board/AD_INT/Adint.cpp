/**
 ****************************************************************************************
 * Program        : ADINTF.CPP                                                          *
 * Description    : Demo program for analog input function with interrupt triggering    *
 *                  (accept user to change FIFO size)                                   *
 * Boards Supp.   : PCL-818 series/816/1800/812PG/711B, MIC-2718, PCM-3718              *
 *                  PCI-1710/1713                                                       *
 * APIs used      : DRV_DeviceOpen,DRV_DeviceClose, DRV_FAIIntStart, DRV_FAIStop,       *
 *                  DRV_FAICheck, DRV_FAITransfer, DRV_GetErrorMessage                  *
 * Revision       : 1.00                                                                *
 * Date           : 7/1/2003                   Advantech Co., Ltd.                      *
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
#include <malloc.h>
#include <conio.h>

#include "C:\Program Files\Advantech\Adsapi\Include\driver.h"

using namespace System;
using namespace System::IO;
using namespace LibTool;

/******************************
 *     Macro definition       *
 ******************************/
#define ERR_SIZE USHORT (-1)
#define TOOLKEY "ebb8b1dbec1a998a061b65b16017cab6896b630e"

/******************************
 * Local function declaration *
 ******************************/
void ErrorHandler(DWORD dwErrCde);
BOOL AllocateDataBuffer(long, int, int, USHORT**, void**);
void FreeDataBuffer(USHORT* pusINTBuf, void *pUserBuf);
void ErrorStop(long*, DWORD, USHORT*, void*, RLF_Manager ^);
USHORT GetFIFOSize(ULONG);

int main()
{
    DWORD  dwErrCde;
    ULONG  lDevNum;
    long   lDriverHandle;
    int    i, iSamples;
    USHORT usChan;
	USHORT usGain;
    String ^ fifo;

    USHORT *pusINTBuf;      //INT internal transfer buffer
    float  *pfUserBuf;      //User buffer for retrieve data
    USHORT usActiveBuf;     //for PT_FAICheck to return data
    USHORT usStopped;
    ULONG  ulRetrieved;
    USHORT usOverrun;
    USHORT usHalfReady;
    ULONG  ulPreRetrieved;
    USHORT usFifoSize;		//Fifo size

    PT_FAIIntStart  tFAIIntStart;
    PT_FAICheck     tFAICheck;
    PT_FAITransfer  tFAITransfer;

	StreamWriter ^ stream = gcnew StreamWriter(Console::OpenStandardOutput());
	stream->AutoFlush = true;
	Console::SetOut(stream);

	// 1. Inicio RLF
	RLF_Manager ^ manager = gcnew RLF_Manager();
	try {
		manager->init(TOOLKEY);
	} catch (RLF_Exception ^ e){
		Console::WriteLine("Error con la base de datos al iniciar. " + e->getMsg());
		stream->Flush();
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

	// 3. Fifo.
	try {
		fifo = manager->getParameter("fifo")->getValue();
	} catch (RLF_Exception ^ e){
		Console::WriteLine("Error con la base de datos al obtener datos." + e->getMsg());
		try {
			manager->finalize(1, "Error.");
		} catch (...){
		}
		return 1;
	}


    if (fifo->CompareTo("true") == 0){
    
        usFifoSize = GetFIFOSize(lDevNum);
        if(usFifoSize == ERR_SIZE)
        {
            Console::WriteLine("Error en la preparación del dispositivo!\n");
            try {
				manager->finalize(1, "Error.");
			} catch (...){
			}
            return 1;
        }
        if(usFifoSize == 0)
        {
            usFifoSize = 1;
        }
        else
        {
			// 4. Tamaño del fifo.
            try {
				usFifoSize = System::UInt16::Parse(manager->getParameter("fifo_size")->getValue());
			} catch (RLF_Exception ^ e){
				Console::WriteLine("Error con la base de datos al obtener datos." + e->getMsg());
				try {
					manager->finalize(1, "Error.");
				} catch (...){
				}
				return 1;
			} catch (...){
				try {
					manager->throwException("FORMAT", "Formato de parámetro fifo_size inválido.");
					manager->finalize(1, "Error.");
				} catch (...){
				}
				return 1;
			}
            if(usFifoSize <= 0) {
				usFifoSize = 1;
			}
        }
    }
    else
    {
        usFifoSize = 1;
    }

	// 5. Número de muestras.
	try {
		iSamples = System::Int32::Parse(manager->getParameter("samples")->getValue());
	} catch (RLF_Exception ^ e){
		Console::WriteLine("Error con la base de datos al obtener datos." + e->getMsg());
		try {
			manager->finalize(1, "Error.");
		} catch (...){
		}
		return 1;
	} catch (...){
		try {
			manager->throwException("FORMAT", "Formato de parámetro samples inválido.");
			manager->finalize(1, "Error.");
		} catch (...){
		}
		return 1;
	}
    
	// 6. Canal.
	try {
		usChan = System::UInt16::Parse(manager->getParameter("channel")->getValue());
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

	// 7. Ganancia.
	try {
		usGain = System::UInt16::Parse(manager->getParameter("gain")->getValue());
	} catch (RLF_Exception ^ e){
		Console::WriteLine("Error con la base de datos al obtener datos." + e->getMsg());
		try {
			manager->finalize(1, "Error.");
		} catch (...){
		}
		return 1;
	} catch (...){
		try {
			manager->throwException("FORMAT", "Formato de parámetro gain inválido.");
			manager->finalize(1, "Error.");
		} catch (...){
		}
		return 1;
	}
	

    // 8. Apertura del dispositivo.
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

    //Step 4: Allocate INT & data buffer for interrupt transfer
    if( AllocateDataBuffer(
            lDriverHandle,              // driver handle
            iSamples,                   // data count
            sizeof(float),              // size of one data
            &pusINTBuf,                 // INT buffer allocated
            (void**)&pfUserBuf)==false) // user buffer allocated
    {
        Console::WriteLine("Error en la apertura del dispositivo.");
        DRV_DeviceClose(&lDriverHandle);

        try {
			manager->finalize(1, "Error.");
		} catch (...){
		}
		return 1;
    }

    // Step 5: Start interrupt transfer
    tFAIIntStart.TrigSrc = 0;          // 0: internal trigger, 1: external trigger
    tFAIIntStart.SampleRate = 10000;    // pacer rate: 10KHz
    tFAIIntStart.chan   = usChan;      // input channel
    tFAIIntStart.gain   = usGain;           // gain code:0, check manual for detail input range
    tFAIIntStart.count  = iSamples;    // number of samples 
    tFAIIntStart.buffer = pusINTBuf;   // data buffer pointer
    tFAIIntStart.cyclic = 0;           // 0: non-cyclic mode, 1: cyclic-mode
    tFAIIntStart.IntrCount = usFifoSize;      //  FIFO interrupt
	
	Console::WriteLine("\nNumber of samples: {0:G}\nSize of Fifo: {1:G}\n",tFAIIntStart.count,tFAIIntStart.IntrCount );

    dwErrCde = DRV_FAIIntStart(lDriverHandle, &tFAIIntStart);
    if (dwErrCde != SUCCESS)
    {
        ErrorStop(&lDriverHandle, dwErrCde, pusINTBuf, (void*)pfUserBuf, manager);
		try {
			manager->finalize(1, "Error.");
		} catch (...){
		}
        return 1;
    }

    // Step 6: Check INT Status
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
        ErrorStop(&lDriverHandle, dwErrCde, pusINTBuf, (void*)pfUserBuf, manager);
        return 1;
    }

    // Step 7: Stop A/D conversion for high speed
    dwErrCde = DRV_FAIStop(lDriverHandle);
    if (dwErrCde != SUCCESS)
    {
//		DRV_FAIStop(lDriverHandle);
        ErrorStop(&lDriverHandle, dwErrCde, pusINTBuf, (void*)pfUserBuf, manager);
        return 1;
    }

    // Step 8: Get data from driver
    tFAITransfer.ActiveBuf = 0;         // 0: single buffer, 1: double buffer
    tFAITransfer.DataType = 1;          // 0: raw or unsigned short data, 1: float data
    tFAITransfer.start    = 0;          // 0: returned starting point of the data buffer
    tFAITransfer.count    = iSamples;   // requested data length. It requests all data.
    tFAITransfer.overrun  = &usOverrun; // returned overrun flag
    tFAITransfer.DataBuffer = pfUserBuf;

    dwErrCde = DRV_FAITransfer(lDriverHandle, &tFAITransfer);
    if (dwErrCde != SUCCESS)
    {
		DRV_FAIStop(lDriverHandle);
        ErrorStop(&lDriverHandle, dwErrCde, pusINTBuf, (void*)pfUserBuf, manager);
        return 1;
    }

    // Step 9: Display data starting from 0 to 99 of buffer location
    for (i = 0; i < 100; i ++)
    {
		Console::Write("Buf[{0:G}] = {1:F}\n", i, pfUserBuf[i]);
    }

    // Step 10: Free buffer
    FreeDataBuffer(pusINTBuf, (void *)pfUserBuf);

    // Step 11: Close device
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

/**********************************************************************
 * Function:   ErrorStop
 *             Release all resource and terminate program if error occurs
 * Paramaters: pDrvHandle, IN/OUT, pointer to Driver handle
 *             dwErrCde, IN, Error code.
 *             plINTBuf, IN, Address of Interrupt buffer
 *             pUserBuf, IN, Address of user buffer
 *			   manager, IN, Gestor de la herramienta.
 * return:     none
 **********************************************************************/
void ErrorStop( long*   pDrvHandle,
                DWORD   dwErrCde,
                USHORT* pusINTBuf,
                void*   pUserBuf,
				RLF_Manager ^ manager)
{
    //Free resource
 //   DRV_FAIStop(*pDrvHandle);
    FreeDataBuffer(pusINTBuf, pUserBuf);

    //Error message
    ErrorHandler(dwErrCde);
    Console::Write("Programa terminado con errores.\n");

    //Close device
    DRV_DeviceClose(pDrvHandle);

    try {
		manager->finalize(1, "Error.");
	} catch (...){
	}
    exit(1);
}//ErrorStop

/**********************************************************************
 * Function:   GetFIFOSize
 *             Get the FIFO size of the device you specified
 * Paramaters: lDevNum, IN, Device number by which to specify device
 * return:     the FIFO size of the specified device, if error occurs
               return ERR_SIZE
 *********************************************************************/
USHORT GetFIFOSize(ULONG lDevNum)
{
    DWORD  dwErrCde;
    long   lDriverHandle;
    long   lFifoSize;

    // Step 1: Open device
    dwErrCde = DRV_DeviceOpen(lDevNum, &lDriverHandle);
    if (dwErrCde != SUCCESS)
    {
        ErrorHandler(dwErrCde);
        return ERR_SIZE;
    }

    // Step 2: Get FIFO size
	dwErrCde = DRV_GetFIFOSize(lDriverHandle, &lFifoSize);
    if(dwErrCde != SUCCESS)
    {
        ErrorHandler(dwErrCde);
        DRV_DeviceClose(&lDriverHandle);
        return ERR_SIZE;
    }
                
    // Step 3: Close device
    DRV_DeviceClose(&lDriverHandle);
                
    // divide by 2 for conversion from byte to word
    return (USHORT)lFifoSize / 2;
}//GetFIFOSize
