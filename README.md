# JAVA_Selenium_crawler
a simple crawler written by JAVA and Selenium, which could be used to grab information from website(tianyancha)

一个简单的爬虫程序，使用JAVA和sleneium（chromedriver）写的，可以从tyc这个网站上抓取数据

first crawler project, first try

第一次尝试

really slow because webdriver is simulate human beings reactions, but unlikely be blocked

selenium 相比于其他工具来讲，因为是模拟人的行为，所以会更慢一些，但也更不容易被发现和被封

unable to be fully automated because of the verification
tried some tools to solves this problem, but did not perform well

还没能解决验证码的问题，试过几个工具但效果不明显，所以只能人工操作

agian, this program is really really really slow...

最后，这个程序真的超级慢...

based on my test, only 4-5/min

大概每分钟四到五条吧。绝了。

I jsut finished a new crawler program which is developed on springboot and use selenium & httpclient. Since I have not find an useable tool to deal with verificatoin code, this program still need manual processing, but, it does much faster than this one. It could grad at least 2000+ data per min based on my test:)
I am testing that program right now and try to fix some bugs. I will upload that program when it feels ready.

最近又完成了一个新的爬虫程序，是用selenium和httpclient写的，基于springboot的环境。依旧是人工处理验证码（对不起这个我真的一时半会做不出来，如果手头上有方便的工具可以自己加到code里试试），但速度比selenium提高了不少，基本上一个小时可以抓2000+的数据，如果开多线程的话速度还可以更快。
这几天在进行测试，完成后会传上来。




10.26

httpclient的程序已经搞出来了，速度比这个要快不少，这一周实际运行的过程中比较稳定，已经取代了这个程序，一上午三个小时能抓5000条（其实大部分时间主要是浪费在验证上了）。不过因为各种原因，现在不方便po上来，所以......。

不过大体思路是有的。主要就是先获取一个cookie，然后设定请求头携带这个cookie去访问，然后再用jsoup进行解析。原理倒是不难，只是刚开始的时候cookie的完整性绊了我一脚。无论是editthecookie 还是开发者模式都无法拿到完整的cookie（也可能是我的姿势有问题），后来用fiddler逐步分析才搞明白cookie的构成。总之不是一个很难的程序，多试试就可以了。

