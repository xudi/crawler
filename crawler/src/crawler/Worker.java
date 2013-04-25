package crawler;

import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Worker implements Runnable {
	private boolean debug = true;
	private Master master;
	private final int oneRoundSleepTime = 200;
	private final int noUrlSleepTime = 1000;
	private final String userAgent = "Jcrawler";
	private String url;

	public Worker(Master master) {
		this.master = master;
	}

	/*
	 * remove non http url, and delete html segment
	 */
	private String constructValidUrl(String linkUrl) {
		String result = linkUrl.trim();
		if (!result.startsWith("http://")) {
			return null;
		}
		
		int last = result.indexOf('#');
		if (last != -1)
			result = result.substring(0, last);
		
		return result;
	}

	private void handleUrl(String url) {
		if (debug) {
			System.out.println(Thread.currentThread().getName() + " is handling '" + url + "'");
		}
		Document doc;
		try {
			doc = Jsoup.connect(url).userAgent(userAgent).get();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return;
		}
		master.writeContent(url, doc.body().text());

		Elements links = doc.select("a");
		Iterator<Element> it = links.iterator();
		while (it.hasNext()) {
			Element link = (Element) it.next();
			String linkUrl = constructValidUrl(link.attr("abs:href"));
			if (linkUrl == null) {
				continue;
			}
			if (link.hasText() && linkUrl.length() > 0) {
				master.writeUrlText(linkUrl, link.text());
				master.addUrl(linkUrl);
			}
		}
	}

	private int oneRound() {
		url = master.getUrl();
		
		if (url == null) {
			switch (master.getStatus()) {
			case Stop:
				return 1;// exit
			case Wait:
				try {
					Thread.sleep(noUrlSleepTime);
				} catch (InterruptedException e) {
					return 0;// continue
				}
				return 0;// continue
			default:
				return 0;// continue
			}
		}
		
		handleUrl(url);
		return 0;
	}

	@Override
	public void run() {
		for (;;) {
			if (oneRound() == 1) {
				System.out.println(Thread.currentThread().getName() + " finished its job");
				return;
			}
			System.out.println(Thread.currentThread().getName() + " finished one round");
			try {
				Thread.sleep(oneRoundSleepTime);
			} catch (InterruptedException e) {
				continue;
			}
		}
	}
}