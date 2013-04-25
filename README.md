crawler
=======

This is a web spider implemented in Java, it fetch a url that you provide, and extrat links in it, and go on fetching,
see [Example](https://github.com/xudi/crawler/blob/master/crawler/src/crawler/Test.java).

When initlizing Master, you should provide four arguments:
* upperBound, the max number of pages you want fetch, this is a soft limitation
* sqlurl, the sqlite url you want the result to stored to
* startUrl, the start point of spider
* numWorkers, number of workers, after start the master, the master will create as many workers as you specified,to
speed up the fetching

TODO
====
* robots.txt support
* sitemap.xml support
