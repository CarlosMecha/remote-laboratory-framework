/**
 * Remote Laboratory Framework
 *
 * version 0.1
 * Carlos A. Rodríguez Mecha
 *
 * Herramienta de visualización en tiempo real de la memoria del sistema.
 */
  
#include "stdafx.h"
#include <windows.h>
#include <stdio.h>
#include <tchar.h>

using namespace System;
using namespace LibTool;
using namespace System::Threading;
using namespace System::IO;

#define DIV 1024
#define WIDTH 7
#define TOOLKEY "e411b1aeaa134aaa01e4489475a42d7747fbdf6b"

int main(array<System::String ^> ^args)
{

	if (args->Length>=1 && args[0]->CompareTo("--clear") == 0){
		return 0;
	}

	int time = 5;
    MEMORYSTATUSEX statex;
	statex.dwLength = sizeof (statex);
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

	// 2. Obtención de la constante de tiempo.
	try {
		time = Convert::ToInt32(manager->getConst("time")->getValue());
	} catch (RLF_Exception ^ e){
		Console::WriteLine("Error con la base de datos al obtener datos." + e->getMsg());
		try {
			manager->finalize(1, "Error.");
		} catch (...){
		}
		return 1;
	}

	try {
		manager->finalize(1, "Error.");
	} catch (...){
		Console::WriteLine("Error con la base de datos al finalizar.");
		return 1;
	}

	// 3. Ejecución.
	while(true){
		GlobalMemoryStatusEx (&statex);
		Console::Write("Memory in use: {0:G}% ({1:D} / {2:D} Kbytes)", statex.dwMemoryLoad, statex.ullAvailPhys/DIV, statex.ullTotalPhys/DIV);
		Thread::Sleep(1000 * time);
	}

    return 0;
}
