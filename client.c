#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <errno.h>
#include <sys/wait.h>
#include <signal.h>
#include <ctype.h>          
#include <arpa/inet.h>

#define MSG_SIZE 80
#define NAME_SIZE 20
#define MAX_SIZE 1000
#define MYPORT 7400
#define LENGTH 1024

#define CFG_ID_KEY 1
#define CFG_NICK_KEY 2

void error(const char *msg)
{
    perror(msg);
    exit(1);
}

char *trimWhitespace(char *str)
{
  char *end;

  // Trim leading space
  while(isspace(*str)) str++;

  if(*str == 0)  // All spaces?
    return str;

  // Trim trailing space
  end = str + strlen(str) - 1;
  while(end > str && isspace(*end)) end--;

  // Write new null terminator
  *(end+1) = 0;

  return str;
}

void analyzeCommand(char* message, char* command, char* receiver, char* content){
  /* init */
  strcpy(receiver, "");
  strcpy(content, "");
  strcpy(command, "");

  if (message == NULL || strlen(message) == 0)
    return;  
  
  if (message[0] == '/'){
    /* start process command */
    sscanf(message+1, "%s %s %[^\n]",command, receiver, content);
    return;
  }

  strcpy(content, message);
}



/* 
   Message format:
   M[receiver][message] -> send simple message
   F[receiver][filelink] -> to transfer file to server
   C[key:value] -> send configure (ex: nickname register)
   N[notify string] -> notify user
   X[message] -> client exit	      

   receiver's length: NAME_SIZE(20)
*/


int main(int argc, char *argv[]) {
  int i=0;
   
  int port;
  int server_sockfd, client_sockfd;
  struct sockaddr_in server_address;
  int addresslen = sizeof(struct sockaddr_in);
  int fd;
  fd_set readfds, testfds, clientfds;
  char msg[MSG_SIZE + 1];     
  char kb_msg[MSG_SIZE + 22]; 
  char revbuf[LENGTH]; 
  char sdbuf[LENGTH]; 
  char pkgbuf[LENGTH+10];
  int fs_block_sz; 
  char fileTransfer[MSG_SIZE];
  FILE *f;
  int isUploading;
   
  /*Client variables=======================*/
  int sockfd;
  int result;
  char nickname[NAME_SIZE];
  char hostname[MSG_SIZE];
  int is_nickname_set;
  struct hostent *hostinfo;
  struct sockaddr_in address;
  int clientid;   

  /*Client==================================================*/
  if(argc==2 || argc==4){
    if(!strcmp("-p",argv[1])){
      if(argc==2){
	printf("Invalid parameters.\nUsage: chat [-p PORT] HOSTNAME\n");
	exit(0);
      }else{
	sscanf(argv[1],"%i",&port);
	strcpy(hostname,argv[3]);
      }
    }else{
      port=MYPORT;
      strcpy(hostname,argv[1]);
    }

    printf("\n*** Client program starting (enter \"quit\" to stop):\n");
    fflush(stdout);
     
    /* Create a socket for the client */
    sockfd = socket(AF_INET, SOCK_STREAM, 0);

    /* Name the socket, as agreed with the server */
    hostinfo = gethostbyname(hostname);  /* look for host's name */
    address.sin_addr = *(struct in_addr *)*hostinfo -> h_addr_list;
    address.sin_family = AF_INET;
    address.sin_port = htons(port);

    /* Connect the socket to the server's socket */
    if(connect(sockfd, (struct sockaddr *)&address, sizeof(address)) < 0){
      perror("connecting");
      exit(1);
    }
    printf("Connected to server, please set a nickname to start chat\n");

    /* init client variable */
    memset(nickname, 0, MSG_SIZE); /* init nickname to zero */
    clientid = -1;
    is_nickname_set = 0;
    isUploading = 0;
     
    fflush(stdout);
     
    FD_ZERO(&clientfds);// init client description files
    FD_SET(sockfd,&clientfds);// add socket client description file
     
    /*  Now wait for messages from the server */
    while (1) {
      testfds=clientfds;
      select(FD_SETSIZE,&testfds,NULL,NULL,NULL);
       
      /* set prompt */
      /*if (nickname[0] != 0)	printf("%s>",nickname);
       */

      for(fd=0;fd<FD_SETSIZE;fd++){
	if(FD_ISSET(fd,&testfds)){	  
	  if(fd==sockfd){   /*Accept data from open socket */	       
	    //read data from open socket
	    result = read(sockfd, msg, MSG_SIZE);

	    if (clientid < 0 || !is_nickname_set){ /* set client id before start chat */
	      msg[result] = '\0'; /* Terminate string with null */
	      if(msg[0] == 'C'){
		int key = -1;
		char temp[MSG_SIZE];
		sscanf(msg, "C%2d:%s", &key, temp);
		switch (key){
		case CFG_ID_KEY: /* configure clientid */
		  clientid = atoi(temp);
		  if (clientid < 0) break;

		  printf("You are signed in with id: %d\n", clientid);		    
		  /* require nickname before start chat */
		  do {
		    printf("Your nickname(no space):");
		    scanf("%s",nickname);
		    fflush(stdout);
		    if (nickname[0] == 0) printf("Please set nickname before start\n");
		    else {
		      /* send nickname to server */
		      sprintf(msg, "C%2d:%s", CFG_NICK_KEY, nickname);
		      write(sockfd, msg, strlen(msg));
		      break;
		    }
		  } while(1);
		  break;
		case CFG_NICK_KEY:
		  printf("Your nickname %s is set. You can start chat now.\n", temp);
		  is_nickname_set = 1;
		  printf("%s>",nickname);
		  fflush(stdout);
		  strcpy(nickname, temp);
		  FD_SET(0,&clientfds); /* add keyboard listen from here */
		  break;
		}	      
	      } else if (msg[0] == 'N'){
		printf("%s\n",msg+1);
		/* require nickname before start chat */
		do {
		  printf("Your nickname(no space):");
		  scanf("%s",nickname);
		  if (nickname[0] == 0) printf("Please set nickname before start\n");
		  else {
		    /* send nickname to server */
		    sprintf(msg, "C%2d:%s", CFG_NICK_KEY, nickname);
		    write(sockfd, msg, strlen(msg));
		    break;
		  }
		} while(1);
	      }
	    } else { /* start show chat when clientid is set */
	      msg[result] = '\0'; /* Terminate string with null */	     
	      char code = msg[0];
	      char temp[MSG_SIZE];
	      char* trimName;
	      int id;
	      switch(code){
	      case 'M':
		strcpy(temp, msg+1);
		temp[NAME_SIZE] = '\0';
		char* trimName = trimWhitespace(temp);
		char sender[NAME_SIZE+1];
		if (strcmp(trimName,"") == 0){
		  strcpy(sender, "Server"); 
		} else {
		  strcpy(sender, trimName);
		}
		printf("\n-->(%s) %s\n",sender, msg+21);
		printf("%s>",nickname);
		fflush(stdout);
		break;
	      case 'N':
		printf("\nWARN: %s\n",msg+1);
		printf("%s>",nickname);
		fflush(stdout);
		break;
	      case 'X':
		printf("%s\n",msg+1);
		close(sockfd); //close the current client
		exit(0);
		break;
	      case 'F': /* receive start signal to transfer file */
		if (!isUploading){ /* check if uploading or not*/
		  /* start transfer id */
		  strncpy(temp, msg+1, 4);
		  temp[4] = '\0';
		  printf("\nstart tranfer file to server...%s|\n",msg);
		  /* open file if need */
		  int error = 0;
		  if (strcmp(fileTransfer, "")!= 0){
		    if ((f = fopen(fileTransfer, "r")) == NULL){
		      printf("Error: Cant open file %s\n", fileTransfer);
		      error = 1;
		    }
		  }
		  if (!error){
		    memset(sdbuf, 0, LENGTH);
		    sprintf(pkgbuf, "T%4s", temp);
		    /* start transfer */
		    isUploading = 1;
		    while((fs_block_sz = fread(sdbuf, sizeof(char), LENGTH, f)) > 0){
		      memcpy(pkgbuf+5, sdbuf, fs_block_sz);
		      write(sockfd, pkgbuf, fs_block_sz+5);
		      memset(sdbuf, 0, LENGTH);
		    }
		    printf("transfer successfully\n");
		    isUploading = 0;
		    strcpy(fileTransfer, "");
		    fclose(f);
		  }
		}else
		  printf("\nfile %s is in uploading\n", fileTransfer);
		printf("%s>",nickname);
		fflush(stdout);
		break;
	      }    
	      memset(msg, 0, MSG_SIZE);
	      memset(kb_msg, 0, MSG_SIZE);
	    }                         	  
	  }
	  else if(fd == 0){ /*process keyboard activiy*/                
	    fgets(kb_msg, MSG_SIZE+1, stdin);
	    // remove \n at end before process
	    kb_msg[strlen(kb_msg)-1] = '\0';
	    if (strcmp(kb_msg, "quit")==0) {
	      sprintf(msg, "XClient[%s] is shutting down.\n",nickname);
	      write(sockfd, msg, strlen(msg));
	      close(sockfd); //close the current client
	      exit(0); //end program
	    }
	    else {
	      if (strlen(kb_msg) > 0){
		char command[MSG_SIZE];
		char content[MSG_SIZE];
		char receiver[NAME_SIZE];
		analyzeCommand(kb_msg, command, receiver, content);
		//printf("analyze %s-%s-%s\n",command, receiver, content);
		if (strcmp(command,"") == 0 || strcmp(command,"to") == 0){
		  /* send message */
		  sprintf(msg, "M%20s%s", receiver,content);
		  write(sockfd, msg, strlen(msg));
		} else if (strcmp(command,"file")==0 && strcmp("",receiver)!=0 && strcmp("",content)!=0){
		  /* communicate before transfer 
		     content in this case is file name*/	
		  strcpy(fileTransfer, content);
		  sprintf(kb_msg, "F%20s%s", receiver, fileTransfer);
		  write(sockfd, kb_msg, strlen(kb_msg));				 
		}
		printf("%s>",nickname);
		fflush(stdout);
	      }
	    }                                                 
	  }          
	}
      }      
    }
  }// end client code   
}//main
