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
#define MAX_CLIENTS 95
#define MAX_SIZE 1000
#define MAX_FILES 50
#define MYPORT 7400
#define LENGTH 1024

#define CFG_ID_KEY 1
#define CFG_NICK_KEY 2

typedef struct tempFileDes{
  int filedes; // use to write and read file
  char name[NAME_SIZE];
  int receiver_id;
  int send_id;
} TempFileDes;

int createTempfile(int sender_id, int receiver_id, char* name, TempFileDes array[], int* num_files);
void error(const char *msg);
void exitClient(int fd, fd_set *readfds, char fd_array[], char* name_array[], int *num_clients, TempFileDes tempfile_array[], int *num_files);
char *trimWhitespace(char *str);

/* 
   Message format:
   M[receiver][message] -> send simple message
   F[receiver][filedescription] -> communicate before transfer
   T[id][package] -> upload file follow id
   D[id][filedescription] -> communicate before download
   C[key]:[value] -> send configure (ex: nickname register)
   N[notify string] -> notify user
   X[message] -> client exit	      

   receiver's length: NAME_SIZE(20)
   key's length: 2   
*/


int main(int argc, char *argv[]) {
  int i=0;
   
  int port;
  int num_clients = 0;
  int num_files = 0;
  int result;
  int server_sockfd, client_sockfd;
  struct sockaddr_in server_address;
  int addresslen = sizeof(struct sockaddr_in);
  int fd;
  char fd_array[MAX_CLIENTS];
  char* client_name_array[MAX_CLIENTS];
  TempFileDes tempfile_array[MAX_FILES];
  int fs_block_sz;
  fd_set readfds, testfds, clientfds;
  char msg[MSG_SIZE + 1];     
  char kb_msg[MSG_SIZE + 22]; 
  char pkgbuf[LENGTH+5]; // read package from client  
  char revbuf[LENGTH]; // receiver buffer 
   

  /*Server==================================================*/
  if(argc==1 || argc == 3){
    if(argc==3){
      if(!strcmp("-p",argv[1])){
	sscanf(argv[2],"%i",&port);
      }else{
	printf("Invalid parameter.\nUsage: server [-p PORT]\n");
	exit(0);
      }
    }else port=MYPORT;
     
    printf("\n*** Server program starting (enter \"quit\" to stop): \n");
    fflush(stdout);

    /* Create and name a socket for the server */
    server_sockfd = socket(AF_INET, SOCK_STREAM, 0);
    server_address.sin_family = AF_INET;
    server_address.sin_addr.s_addr = htonl(INADDR_ANY);
    server_address.sin_port = htons(port);
    bind(server_sockfd, (struct sockaddr *)&server_address, addresslen);

    /* Create a connection queue and initialize a file descriptor set */
    listen(server_sockfd, 1);
    FD_ZERO(&readfds);
    FD_SET(server_sockfd, &readfds);
    FD_SET(0, &readfds);  /* Add keyboard to file descriptor set */     

    /* init server variable */
    memset(client_name_array, 0, sizeof(char*)*MAX_CLIENTS);
    memset(tempfile_array, 0, sizeof(TempFileDes)*MAX_FILES);
    memset(pkgbuf, 0, LENGTH+5);
    

    /*  Now wait for clients and requests */
    while (1) {
      testfds = readfds;
      select(FD_SETSIZE, &testfds, NULL, NULL, NULL);
                    
      /* If there is activity, find which descriptor it's on using FD_ISSET */
      for (fd = 0; fd < FD_SETSIZE; fd++) {
	if (FD_ISSET(fd, &testfds)) {              
	  if (fd == server_sockfd) { /* Accept a new connection request */
	    client_sockfd = accept(server_sockfd, NULL, NULL);
                                
	    if (num_clients < MAX_CLIENTS) {
	      // Add new clientfd to file descriptor set
	      FD_SET(client_sockfd, &readfds);
	      fd_array[num_clients]=client_sockfd;
	      /*Client ID*/
	      printf("Client %d joined\n",num_clients++);
	      fflush(stdout);
                    
	      sprintf(msg,"C%2d:%2d", CFG_ID_KEY, client_sockfd);
	      printf("Send clientID to client:%s\n",msg);
	      /*write 2 byte clientID */
	      write(client_sockfd,msg,strlen(msg));
	    } else {
	      sprintf(msg, "XSorry, too many clients.  Try again later.\n");
	      write(client_sockfd, msg, strlen(msg));
	      close(client_sockfd);
	    }
	  } else if (fd == 0)  {  /* Process keyboard activity */                 
	    fgets(kb_msg, MSG_SIZE + 1, stdin);
	    kb_msg[strlen(kb_msg)-1] = '\0';
	    if (strcmp(kb_msg, "quit")==0) {
	      sprintf(msg, "XServer is shutting down.\n");
	      for (i = 0; i < num_clients ; i++) {
		write(fd_array[i], msg, strlen(msg));
		close(fd_array[i]);
	      }
	      close(server_sockfd);
	      exit(0);
	    }
	    else {
	      sprintf(msg, "M%20s%s", " ",kb_msg);
	      for (i = 0; i < num_clients ; i++)
		write(fd_array[i], msg, strlen(msg));
	    }
	  } else if(fd) {  /*Process Client specific activity*/	    
	    //read data from open socket
	    result = read(fd, pkgbuf, LENGTH+5);	    
                 
	    if(result==-1) perror("read()");
	    else if(result>0){
	      /* if transfer file, use pkgbuf directly, 
		 other case will use msg with smaller size */
	      if (result < 80)
		printf("Server receive:%s|\n", pkgbuf);
	      int id = -1, check;       
	      char code = pkgbuf[0];
	      char name[MSG_SIZE];
	      char temp[MSG_SIZE];
	      char* trimName;
	      memset(name, 0, MSG_SIZE);
	      
	      switch(code){
	      case 'M': /* receive message from client */
		memcpy(msg, pkgbuf, MSG_SIZE); /* use msg buffer */
		msg[result]='\0';
		if (strlen(msg+21) > 0){  /* drop message with no length */
		  /* find client nickname */
		  for (i=0;i<num_clients;i++){
		    if (fd_array[i] == fd){ 
		      strcpy(name, client_name_array[i]);
		    }
		  }
		  if (name[0] == 0){
		    printf("Client name not found\n");
		    break;
		  }
		     
		  /*concatinate the client id with the client's message*/
		  sprintf(kb_msg, "M%20s%s", name, msg+21);
                    
		  strcpy(temp, msg+1);
		  temp[NAME_SIZE] = '\0';
		  char* trimName = trimWhitespace(temp);
		  if (strcmp("", trimName) == 0){
		    /*print to other clients*/
		    for(i=0;i<num_clients;i++){
		      if (fd_array[i] != fd)  /*except sender client*/
			write(fd_array[i],kb_msg,strlen(kb_msg));
		    }
		  } else {
		    /* find receiver */
		    for(i=0;i<num_clients;i++)
		      if (client_name_array[i] != NULL)
			if(strcmp(trimName, client_name_array[i]) == 0)
			  id = fd_array[i];		
		      
		    if (id < 0){ /* cant find receiver */
		      sprintf(kb_msg, "N%s", "Cant find receiver");
		      write(fd, kb_msg, strlen(kb_msg));
		      break;
		    }
		    write(id, kb_msg, strlen(kb_msg));
		  }
		  /*print to server*/
		  printf("sent:%s|\n",kb_msg);
		}
		break;
	      case 'X':
		exitClient(fd,&readfds, fd_array, client_name_array, &num_clients, tempfile_array, &num_files);
		break;
	      case 'C':
		memcpy(msg, pkgbuf, MSG_SIZE); /* use msg buffer */
		printf("Configure for client %d\n",fd);
		memset(name, 0, NAME_SIZE);
		sscanf(msg, "C%2d:%s", &id, name);
		trimName = trimWhitespace(name);
		/* check name */
		if (strlen(name) == 0){
		  write(fd, "NInvalid name", 13);
		  break;
		}
		check = 1;
		for(i=0;i<num_clients;i++){
		  if (client_name_array[i] != NULL){		   
		    if (!strcmp(name, client_name_array[i])) {
		      check = 0;
		      break;
		    }
		  } 
		}
		printf("Name: %s, %s\n", name, 1?"ok":"not valid");
		if (check){ /* name is ok */
		  sprintf(kb_msg, "C%2d:%s", CFG_NICK_KEY, name);
		  /* add name to client_name_array */
		  for (i=0;i<num_clients;i++){
		    if (fd_array[i] == fd){
		      client_name_array[i] = (char*)malloc(strlen(name)+1);
		      strcpy(client_name_array[i], name);
		      break;
		    }
		  } 
		  write(fd,kb_msg,strlen(kb_msg));
		} else { /* name is duplicated */
		  sprintf(kb_msg, "N%s", "Name is selected by another client");
		  write(fd,kb_msg,strlen(kb_msg));
		}
		break;
	      case 'F': /* communicate before transfer */
		memcpy(msg, pkgbuf, MSG_SIZE);
		msg[strlen(msg)] = '\0';
		strcpy(name, msg+1);
		strcpy(temp, msg+21);
		name[NAME_SIZE] = '\0';
		trimName = trimWhitespace(name);
		/* find receiver_id */
		int recv_id = -1;
		for(i=0;i<num_clients;i++)
		  if (client_name_array[i] != NULL)
		    if(strcmp(trimName, client_name_array[i]) == 0){
		      recv_id = fd_array[i];
		      break;
		    }
		if (recv_id < 0){
		  write(fd, "NInvalid receiver", 17);
		  break;
		}

		/* check if connect is establish */
		id = -1;
		for (i=0;i<num_files;i++){
		  if (tempfile_array[i].send_id == fd)
		    if (tempfile_array[i].receiver_id == recv_id)
		      { id = i; break;}
		}
		
		if (id < 0 || strcmp(temp, tempfile_array[i].name)!=0){	  
		  id = createTempfile(fd, recv_id, temp, tempfile_array, &num_files);
		  if (id < 0){ /* create temp file fail*/
		    sprintf(kb_msg, "N%s", "Fail to create temp file on server");
		    write(fd, kb_msg,  strlen(kb_msg));
		    break;
		  }
		  sprintf(kb_msg, "F%4d%s", id, temp);
		  write(fd, kb_msg, strlen(kb_msg));
		} else {
		  sprintf(msg, "N%s", "This file is uploaded by you");
		  write(fd, msg, strlen(msg));
		}
		fflush(stdout);
		break;
	      case 'T': /* start transfer file */
		/* get tempfile info */
		memcpy(temp, pkgbuf+1, 4);
		temp[4] = '\0';
		id = atoi(temp);
		printf("Result-%d\n",result);
		/* validate info */
		if (tempfile_array[id].send_id != fd) break;
		int filedes = tempfile_array[id].filedes;
		if (filedes == -1) break;
		
		if((i = write(filedes,pkgbuf+5, result-5)) == -1){
		  printf("write failed with error [%s]\n", strerror(errno));
		}
		if (LENGTH + 5 > result) { /* transfer is ended, notify user */
		  /* validate receiver_id */
		  int check = 0;
		  for(i=0;i<num_clients;i++)
		    if (fd_array[i] == tempfile_array[id].receiver_id){
		      check = 1; break;
		    }
		  if (!check){
		    sprintf(msg, "NCant find receiver anymore, file %s will be deleted", tempfile_array[id].name);
		    write(fd, msg, strlen(msg));
		    close(tempfile_array[id].filedes);
		  } else {
		    sprintf(msg, "NYou can down load file %s now with file fileid: %d", tempfile_array[id].name, id);
		    write(tempfile_array[id].receiver_id, msg,  strlen(msg));
		  }
		}
		break;
	      case 'D': /* download file use id */
		/* get tempfile info */
		memcpy(temp, pkgbuf+1, 4);
		temp[4] = '\0';
		id = atoi(temp);

		/* validate info */
		if (tempfile_array[id].receiver_id != fd) break;
		filedes = tempfile_array[id].filedes;
		if (filedes == -1) break;
		
		lseek(filedes,0,SEEK_SET); /* rewind */
		sprintf(pkgbuf, "D%4d%s", id, tempfile_array[id].name);

		/* send file info first */
		write(fd, pkgbuf, strlen(pkgbuf));

		memset(revbuf, 0, LENGTH);
		sprintf(pkgbuf, "T%4d", id);
		while((fs_block_sz = read(filedes, revbuf, LENGTH)) > 0){
		  memcpy(pkgbuf+5, revbuf, fs_block_sz);
		  write(fd, pkgbuf, fs_block_sz+5);
		  memset(revbuf, 0, LENGTH);
		}
		printf("transfer successfully\n");		
		break;
	      default:
		break;
	      }                                  	    
	      memset(pkgbuf, 0, LENGTH+5);
	    } 
	  } else {  /* A client is leaving */
	    exitClient(fd,&readfds, fd_array, client_name_array, &num_clients, tempfile_array, &num_files);
	  }//if
	}//if
      }//for
    }//while
  }//end Server code  
}//main


void error(const char *msg){
  perror(msg);
  exit(1);
}

void exitClient(int fd, fd_set *readfds, char fd_array[], char* name_array[], int *num_clients, TempFileDes tempfile_array[], int *num_files){
  int i,j;
    
  close(fd);
  FD_CLR(fd, readfds); //clear the leaving client from the set
  for (i = 0; i < (*num_clients) - 1; i++)
    if (fd_array[i] == fd)
      break;          
  for (; i < (*num_clients) - 1; i++){
    (fd_array[i]) = (fd_array[i + 1]);
    strcpy(name_array[i], name_array[i+1]);
  }
  i = 0;
  while (i <= (*num_files) - 1){
    if (tempfile_array[i].send_id == fd || tempfile_array[i].receiver_id == fd){
      close(tempfile_array[i].filedes);
      tempfile_array[i].filedes = -1;
      for (j=i;j<(*num_files)-1;j++){
	tempfile_array[j] = tempfile_array[j+1];
      }
      (*num_files)--;
    } else i++;
  }
  (*num_clients)--;
}

char *trimWhitespace(char *str){
  char *end;

  // Trim leading space
  while(isspace(*str)) str++;

  if(*str == 0)  return str;

  // Trim trailing space
  end = str + strlen(str) - 1;
  while(end > str && isspace(*end)) end--;

  // Write new null terminator
  *(end+1) = 0;

  return str;
}

int createTempfile(int sender_id, int receiver_id, char* name, TempFileDes array[], int* num_files){
  TempFileDes temp;
  char tempName[MSG_SIZE];
  /* create tempfile */
  strcpy(tempName, "./temp_XXXXXX");
  int filedes = mkstemp(tempName);
  //unlink(tempName);

  if(filedes<1){
    printf("\n Creation of temp file failed with error [%s]\n",strerror(errno));
    return -1;
  }
  printf("a tempfile create at %s\n",tempName);  

  /* add tempfile description */
  strcpy(temp.name, name);
  temp.receiver_id = receiver_id;
  temp.send_id= sender_id;
  temp.filedes = filedes;
  array[(*num_files)++] = temp;
  return *num_files - 1;
}


  
