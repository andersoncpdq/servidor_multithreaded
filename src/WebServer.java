import java.io.*;
import java.net.*;
import java.util.*;

public final class WebServer {

	private static ServerSocket socket;

	public static void main(String[] args) throws Exception {
		
		// Ajusta numero da porta fixa, na qual o servidor escutara as requisicoes.
		int port = 6589;
		
		socket = new ServerSocket(port);
		
		System.out.println("Esperando por requisicoes...");
		System.out.println();
		
		// Processa cada requisição de servico HTTP indefinidamente.
		while(true) {
			// Escuta a requisicao de conexao TCP.
			Socket connection = socket.accept();
			
			// Objeto do tipo HttpRequest que processa a mensagem de requisicao HTTP.
			HttpRequest request = new HttpRequest(connection);
			
			// Cria novo thread para processar a requisicao.
			Thread thread = new Thread(request);
			// Inicia a nova thread.
			thread.start();
		}
	}
}

final class HttpRequest implements Runnable {
	
	// fim de cada linha da mensagem de resposta do servidor.
	final static String CRLF = "\r\n";
	
	// armazena uma referencia ao socket de conexao.
	Socket socket;
	
	String root = "/home/anderson/workspace/Servidor_Multithreaded/src";
	
	// Construtor.
	public HttpRequest(Socket conn) throws Exception {
		this.socket = conn;
	}

	// Implementa o método run() da interface Runnable.
	@Override
	public void run() {
		
		try {
			processRequest();
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	private void processRequest() throws Exception {
		
		// Referencias para entrada e saida do socket.
		InputStream is = socket.getInputStream();
		DataOutputStream os = new DataOutputStream(socket.getOutputStream());
		
		// Ajuste dos filtros de entrada.
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		// Obtendo linha de requisicao da mensagem de requisicao HTTP.
		String requestLine = br.readLine();
		
		// Extrair o nome do arquivo da linha de requisicao.
		StringTokenizer tokens = new StringTokenizer(requestLine);
		
		// Nome do arquivo.
		String fileName = null;
		
		// Metodo deve ser GET.
		if(tokens.nextToken().equals("GET")) {
			fileName = tokens.nextToken();
			fileName = root + fileName;
		} else {
			System.out.println("Bad Request!");
			return;
		}
		
		// Abrir o arquivo requisitado.
		FileInputStream fis = null;
		boolean fileExists = true;
		try {
			fis = new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			fileExists = false;
		}
		
		// Exibir cabecalho no terminal.
		System.out.println();
		System.out.println("Arquivo solicitado: " + fileName);
		System.out.println(requestLine);
		String headerLine = null;
		while ((headerLine = br.readLine()).length() != 0) {
			System.out.println(headerLine);
		}
		
		// Construindo mensagem de resposta.
		// linha de status.
		String statusLine = null;
		// cabecalhos da resposta.
		String contentTypeLine = null;
		// corpo da entidade.
		String entityBody = null;
		
		if(fileExists) {
			statusLine = "HTTP/1.0 200 OK" + CRLF;
			contentTypeLine = "Content-Type: " + contentType(fileName) + CRLF;
		} else {
			statusLine = "HTTP/1.0 404 Not Found" + CRLF;
			contentTypeLine = "Content-Type: text/html" + CRLF;
			entityBody = "<HTML>" + "<HEAD><TITLE>Not Found</TITLE></HEAD>" + "<BODY>Not Found</BODY>" + "</HTML>";
		}
		
		// Enviar linha de status.
		os.writeBytes(statusLine);
		// Enviar linha de tipo de conteudo.
		os.writeBytes(contentTypeLine);
		// Enviar uma linha em branco para indicar o fim das linhas de cabecalho.
		os.writeBytes(CRLF);
		
		// Enviar o corpo da entidade.
		if (fileExists) {
			sendBytes(fis, os);
			fis.close();
		} else {
			os.writeBytes(entityBody);
		}

		// Fechando cadeias e socket.
		os.close();
		br.close();
		socket.close();
	}

	private void sendBytes(FileInputStream fis, DataOutputStream os) throws Exception {
		
		// Buffer de 1KB para comportar os bytes no socket.
		byte[] buffer = new byte[1024];
		int bytes = 0;
		
		// Copiar arquivo requisitado para cadeia de saida do socket.
		while ((bytes = fis.read(buffer)) != -1) {
			os.write(buffer, 0, bytes);
		}
	}

	private String contentType(String fileName) {
		
		if(fileName.endsWith(".htm") || fileName.endsWith(".html")) {
			return "text/html";
		}
		
		if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
			return "image/jpeg";
		}
		
		return "application/octet-stream";
	}
}
