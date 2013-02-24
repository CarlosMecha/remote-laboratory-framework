/**
 * Remote Laboratory Framework Music
 * 
 * by Carlos A. Rodríguez Mecha
 * v0.1
 *
 * Reproductor de música para sistemas Linux.
 */

// Librerías:
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include "lib/libtool.h"

// Macros:
#define FILE1 "music/file1.mp3"
#define FILE2 "music/file2.mp3"
#define FILE3 "music/file3.mp3"
#define INSIZE 100
#define NAMESIZE 50

// Variables:
/** Clave de acceso. */
char * toolkey = "7ec9f83b3d1957d31ed96ec5a04504709597d410";
/** Proceso hijo. */
pid_t player;
/** Reproductor. */
char * command_player = "mpg123";
/** Comandos. */
char * cfile1 = "play chillout";
char * cfile2 = "play rock";
char * cfile3 = "play pop";
char * cstop = "stop";
char * cexit = "exit";


int main (int argc, const char* argv[]) {
	
	int end = 0;
	int killprocess = 0;
	int error = 0;
	int status;
	char * in;
	char * file;
	
	if (argc > 1 && strcmp(argv[1], "--clear") == 0){
		execlp("killall", "killall", command_player, NULL);
		exit(0);
	}
	
	if (RLF_Init(toolkey) != RLF_SUCCESS) {
		fprintf(stderr, "[ERROR] Problema con la base de datos.");
		fflush(stderr);
		exit(1);
	}
	
	fprintf(stdout, "-- RLF Music Player v0.1--\n");
	
	if ((in = (char *) malloc (INSIZE * sizeof(char))) == NULL){
		RLF_ThrowException("MEMEXCEPTION", "Problema con la memoria.");
		RLF_Finalize(1, "Error.");
		exit(1);
	}
	
	if ((file = (char *) malloc (NAMESIZE * sizeof(char))) == NULL){
		RLF_ThrowException("MEMEXCEPTION", "Problema con la memoria.");
		RLF_Finalize(1, "Error.");
		free(in);
		exit(1);
	}
	
	while (!end){
		
		fprintf(stdout, "Para reproducir un fichero, escriba los siguientes comandos:\n");
		fprintf(stdout, "- file 1: %s\n", cfile1);
		fprintf(stdout, "- file 2: %s\n", cfile2);
		fprintf(stdout, "- file 3: %s\n", cfile3);
		fprintf(stdout, "Para salir teclee \"%s\"\n", cexit);
		fflush(stdout);
		
		bzero((void *) file, NAMESIZE * sizeof(char));
		bzero((void *) in, INSIZE * sizeof(char));
		
		fgets (in, INSIZE, stdin);
		in[strlen(in) - 1] = '\0';
		
		if (strcmp(in, cexit) == 0){
			end = 1;
		} else if (strcmp(in, cfile1) == 0){
			strcpy(file, FILE1);
		} else if (strcmp(in, cfile2) == 0){
			strcpy(file, FILE2);
		} else if (strcmp(in, cfile3) == 0){
			strcpy(file, FILE3);
		} else {
			fprintf(stdout, "Comando no reconocido.\n");
			fflush(stdout);
			continue;
		}
		
		if (!end){
			
			player = fork();
			if (player < 0) {
				RLF_ThrowException("MEMEXCEPTION", "Problema con la memoria.");
				error = 1;
				end = 1;
			// 1. Player.
			} else if (player == 0) {
				close(STDIN_FILENO);
				close(STDOUT_FILENO);
				close(STDERR_FILENO);
				execlp(command_player, command_player, "-q", file, NULL);
			// 2. Padre.
			} else {
				killprocess = 0;
				fprintf(stdout, "Para parar la reproducción y escuchar otra música, teclee \"%s\"\n", cstop);
				fflush(stdout);
				
				while (!killprocess){
					bzero((void *) in, INSIZE * sizeof(char));
					fgets (in, INSIZE, stdin);
					in[strlen(in) - 1] = '\0';
					
					if (strcmp(in, cstop) == 0){
						killprocess = 1;
						kill(player, SIGTERM);
					} else {
						fprintf(stdout, "Comando no reconocido.\n");
						fflush(stdout);
					}
				}
				wait(&status);
			}
		}
		
	}
	
	if (error){
		RLF_Finalize(1, "Error.");
	} else {
		RLF_Finalize(0, "Correcto.");
	}
	
	free(in);
	free(file);
	fprintf(stdout, "Terminado\n");
	fflush(stdout);
	exit(0);
	
}
