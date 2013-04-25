package crawler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Master implements Runnable {
	private final int toBeProcessUpperSize = 1000; 
	private MasterStatus status;
	private int upperBound;
	private ConcurrentLinkedQueue<String> toBeProcess = new ConcurrentLinkedQueue<String>();
	private Connection conn;
	private ArrayList<Thread> workerList = new ArrayList<Thread>();
	private Set<String> visited = Collections.synchronizedSet(new HashSet<String>());
	private int numWorkers;
	/*
	 * FIXME: maybe we should add a notify mechanism, instead of letting worker sleep for 1 sec
	 * */
	
	public Master(int upperBound, String sqlurl, String startUrl, int numWorkers) throws Exception {
		Class.forName("org.sqlite.JDBC");
		conn = DriverManager.getConnection(sqlurl);
		Statement stmt = conn.createStatement();
		try {
			stmt.executeUpdate("create table urlText (url, anchor);");
			stmt.executeUpdate("create table content (url, text);");
		} catch (SQLException e) {
			if (!e.getMessage().contains("already exists"))
				throw e;
		}
        
		this.upperBound = upperBound;
		
		if (!startUrl.startsWith("http://") || startUrl.contains("#"))
			throw new UrlException("Url must start with 'http://' and do not contains '#'");
		toBeProcess.add(startUrl.trim());
		
		status = MasterStatus.Run;
		
		this.numWorkers = numWorkers;
	}
	
	public String getUrl() {
		String result = null;
		/* 
		 * if two thread concurrently access this function, maybe they
		 * will makes visited.size() > (upperBound + 1), so upperBound
		 * is not considered as hard limitation.
		 * */
		if (visited.size() > upperBound) {
			status = MasterStatus.Stop;
			return null;
		}
		for (;;) {
			/* we sync because we want two worker do not get same url */
			synchronized(this) {
				result = toBeProcess.poll();
				if (result == null)
					return null;
				else if (result != null && !isVisited(result)) {
					visited.add(result);
					return result;
				} else
					continue;
			}
		}
	}
	
	public MasterStatus getStatus() {
		return status;
	}
	
	private boolean isVisited(String url) {
		return visited.contains(url);
	}
	
	public void addUrl(String normalUrl) {
		if (status == MasterStatus.Stop ||
				toBeProcess.size() > toBeProcessUpperSize) {
			return;
		} else {
			toBeProcess.add(normalUrl);
			synchronized(this) {
				if (status != MasterStatus.Stop)
					status = MasterStatus.Run;
			}
		}
	}
	
	public void writeUrlText(String normalUrl, String anchor){
		try {
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO urlText (url, anchor) values (?, ?);");
			stmt.setString(1, normalUrl);
		    stmt.setString(2, anchor);
		    stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void writeContent(String url, String content) {
		try {
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO content (url, text) values (?, ?);");
			stmt.setString(1, url);
		    stmt.setString(2, content);
		    stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		for (int i = 0; i < numWorkers; i++) {
			Worker w = new Worker(this);
			Thread t = new Thread(w);
			t.setName("Worker" + i);
			t.start();
			workerList.add(t);
		}
		/*FIXME we should detect that all worker is hanging, and toBeProcess is empty */
		while (status != MasterStatus.Stop) {
			System.out.println("already got "+ visited.size() + " pages," + toBeProcess.size() +" to go.");
			try {
				Thread.sleep(4000);
				int i = 0;
				while (i < numWorkers) {
					Thread worker = workerList.get(i);
					if (!worker.isAlive()) {
						workerList.remove(i);
						worker.join();
						numWorkers--;
						continue;
					}
					i++;
				}
				if (workerList.isEmpty()) {
					System.err.println("all child is dead");
					System.exit(1);
				}
			} catch (InterruptedException e) {
				continue;
			}
		}
		for (int i = 0; i < numWorkers;) {
			try {
				workerList.get(i).join();
			} catch (InterruptedException e) {
				continue;
			}
			i++;
		}
		System.out.println("all workers are done");
	}
}