package crawler;

public enum MasterStatus {
	Stop,//reach max number of pages
	Wait,//no url in queue right now
	Run
}
