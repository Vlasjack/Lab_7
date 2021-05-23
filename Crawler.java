import java.lang.Exception;
import java.util.*;
import java.net.MalformedURLException;
import java.net.*;
import java.io.*;

public class Crawler {

	public static final int HTTP_PORT = 80;
	public static final String HOOK_REF = "<a href=\"";
	public static final String BAD_REQUEST_LINE = "HTTP/1.1 400 Bad Request";

	LinkedList<URLDepthPair> notVisitedList;
	LinkedList<URLDepthPair> visitedList;

	int depth;

	public Crawler() {
		notVisitedList = new LinkedList<URLDepthPair>();
		visitedList = new LinkedList<URLDepthPair>();
	}


	public static void main (String[] args) {

		Crawler crawler = new Crawler();

		crawler.getFirstURLDepthPair(args);
		crawler.startParse();
		crawler.showResults();
	}


	public void startParse() {
		System.out.println("Stating parsing:\n");

		URLDepthPair nowPage = notVisitedList.getFirst();

		while (nowPage.getDepth() <= depth && !notVisitedList.isEmpty()) {
			nowPage = notVisitedList.getFirst();
			Socket socket = null;
			
			try {
				socket = new Socket(nowPage.getHostName(), HTTP_PORT);
				System.out.println("Connection to [ " + nowPage.getURL() + " ] created!");
				try {
					socket.setSoTimeout(5000);
				}
				catch (SocketException exc) {
					System.err.println("SocketException: " + exc.getMessage());
					moveURLPair(nowPage, socket);
					continue;
				}

				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

				out.println("GET " + nowPage.getPagePath() + " HTTP/1.1");
				out.println("Host: " + nowPage.getHostName());
				out.println("Connection: close");
				out.println("");

				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String line = in.readLine();

				if (line.startsWith(BAD_REQUEST_LINE)) {
					System.out.println("ERROR: BAD REQUEST!");
					System.out.println(line + "\n");

					this.moveURLPair(nowPage, socket);
					continue;
				} else {
					System.out.println("REQUEST IS GOOD!");
				}

				int strCount = 0;
				int strCount2 = 0;
				while(line != null) {
					try {
						line = in.readLine();
						strCount += 1;
						String url = CrawlerHelper.getURLFromHTMLTag(line);
						if (url == null) continue;

						if (url.startsWith("../")) {
							String newUrl = CrawlerHelper.urlFromBackRef(nowPage.getURL(), url);
							this.createURlDepthPairObject(newUrl, nowPage.getDepth() + 1);
						}
						else if (url.startsWith("http://")) {
							String newUrl = CrawlerHelper.cutTrashAfterFormat(url);
							this.createURlDepthPairObject(newUrl, nowPage.getDepth() + 1);
						}
						else {		
							String newUrl;
							newUrl = CrawlerHelper.cutURLEndFormat(nowPage.getURL()) + url;
							
							this.createURlDepthPairObject(newUrl, nowPage.getDepth() + 1);
						}
						
						strCount2 += 1;
					}
					catch (Exception e) {
						break;
					}
				}
				
				if (strCount == 1) System.out.println("No http refs in this page!");
				System.out.println("Page had been closed\n");
				
			}
			catch (UnknownHostException e) {
				System.out.println("Opps, UnknownHostException catched, so [" + nowPage.getURL() + "] is not workable now!");
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			

			moveURLPair(nowPage, socket);
			
			nowPage = notVisitedList.getFirst();
		}
	}

	private void moveURLPair(URLDepthPair pair, Socket socket) {
		this.visitedList.addLast(pair);
		this.notVisitedList.removeFirst();
		
		if (socket == null) return;
		
		try {
			socket.close();
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();	
		}
	}

	private void createURlDepthPairObject(String url, int depth) {
		
		URLDepthPair newURL = null;
		try{
			newURL = new URLDepthPair(url, depth);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		notVisitedList.addLast(newURL);
	}

	public LinkedList<URLDepthPair> getVisitedSites() {
		return this.visitedList;
	}
	public LinkedList<URLDepthPair> getNotVisitedSites() {
		return this.notVisitedList;
	}

	public void showResults() {
		System.out.println("---Results of working---");

		System.out.println("Scanner scanned next sites:");
		int count = 1;
		for (URLDepthPair pair : visitedList) {
			System.out.println(count + " |  " + pair.toString());
			count += 1;
		}
		System.out.println("-----End of results-----");
	}

	public void getFirstURLDepthPair(String[] args) {
		CrawlerHelper help = new CrawlerHelper();

		URLDepthPair urlDepth = help.getURLDepthPairFromArgs(args);
		if (urlDepth == null) {
			System.out.println("Args are empty or have exception. Now you need to enter URL and depth manually!\n");

			urlDepth = help.getURLDepthPairFromInput();
		}

		this.depth = urlDepth.getDepth();
		urlDepth.setDepth(0);

		notVisitedList.add(urlDepth);

		System.out.println("First site: " + urlDepth.toString() + "\n");
	}

}