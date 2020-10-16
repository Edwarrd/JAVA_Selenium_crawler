package Web_Crawler;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.io.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


//法人
//联系电话，网址，邮箱

public class Web_tyc {

    public static Logger log = LogManager.getLogger(Web_tyc.class.getName());

    //运行抓取程序
    public static void main(String[] args){

        //****用ChromeDriverService来控制ChromeDriver的进程****
        ChromeDriverService service = new ChromeDriverService.Builder()
                .usingDriverExecutable(new File("src/main/resources/res/chromedriver.exe"))
                .usingAnyFreePort().build();


        try {
            //ChromeDriverService启动
            log.info("************     ChromeDriverService 启动        **************");
            service.start();
            try {
                //********读取目标文件**********
                String file_path = readProperties("file_path");//读取config文件
                ArrayList<String> names = readTxt(file_path);//读取comp_name.txt文件

                //*******设置代理ip***********
                log.info("************     WebDriver 启动     *************");
//                WebDriver driver = new RemoteWebDriver(service.getUrl(),
//                        new ChromeOptions().addArguments("--proxy-server=http://" + readProperties("ip")));
//               @@@@@@@@@ip地址切换（换成原ip）

                ChromeOptions options = new ChromeOptions();
                //options.addArguments("user-data-dir=C:/Users/HM/AppData/Local/Google/Chrome/User Data");
                WebDriver driver = new RemoteWebDriver(service.getUrl(),options);

                String URL = readProperties("url");
                driver.get(URL);
                driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);

                //*******判断是否有弹窗出现，有则关闭**********
                String pop_up = "div#_modal_container .modal-close.tic.tic-guanbi1";
                if (WebElementExist(driver,By.cssSelector(pop_up))){
                    driver.findElement(By.cssSelector(pop_up)).click();
                }

                //*****整理公司名称----->读取过的就不再读取

                //先进行登录（测试中
                driver.findElement(By.cssSelector("#web-content>.tyc-home>.tyc-home-top.bgtyc>.mt-74>.tyc-header.-home>.container.rel>.right>.tyc-nav>div:last-child>.link-white")).click();
                log.info("登录页面已找到");
                signin(driver);

                //********遍历所给名字，在tyc上进行查询*********
                if(names!= null){
                    for (String name:names){
                            search(driver, name);
                            write_last_name(readProperties("recording_file_path"), name);
                    }
                }


                driver.close();
                log.info("******************    WebDriver 关闭       *********************");
            } catch (Exception e) {
                log.error(" 读取目标文件失败   "+e);
            }
        } catch (Exception e) {
            log.error(" ChromeService 启动失败  "+e);
        }
        finally {
            //任务结束,关闭ChromeDriverService
            log.info("**********     ChromeDriverService 关闭         ***********");
            service.stop();
        }
    }

    //根据公司名称查找,将结果写入TXT文件中
    private static void search(WebDriver driver, String name){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try{
            //搜索公司名称,搜索框清空+输入+回车
            WebElement searchBar = driver.findElement(By.cssSelector(".js-live-search-auto"));
            searchBar.clear();
            searchBar.sendKeys(name);
            searchBar.sendKeys(Keys.ENTER);
            driver.manage().timeouts().implicitlyWait(500, TimeUnit.MILLISECONDS);

            //**检查是否需要登录**
            check_sign_in(driver);

            //**检查是否有验证码窗口弹出
            check_Verifycode(driver);

            try{
                //**如果搜索没有得到结果，刷新重试
                WebElement first_comp;
                    if (WebElementExist(driver,By.cssSelector(".result-list.no-result"))){
                        for (int i=0;i<random(3);i++){
                            driver.navigate().refresh();
                            if (!WebElementExist(driver,By.cssSelector(".result-list.no-result"))){
                                break;
                            }
                        }
                    }
                    //**选择第一个公司
                if (WebElementExist(driver,By.cssSelector(".result-list.sv-search-container>div:first-child .content .header .name"))) {
                    first_comp = driver.findElement(By.cssSelector(".result-list.sv-search-container>div:first-child .content .header .name"));

                    String first_comp_name = first_comp.getText();
                    //**判断公司名称是否符合
                    System.out.println(df.format(System.currentTimeMillis())+"查询到的公司名称是：" + first_comp_name + "； 查询名称为：" + name);
                    if (first_comp_name.equals(name)) {
                        System.out.println("公司名称匹配，写入文件");
                        String Comp_Url = first_comp.getAttribute("href");
                        String Type_comp = driver.findElement(By.cssSelector(".result-list.sv-search-container>div:first-child .content .tag-list .tag-common.-primary.-new")).getText();
                        //跳入公司详情页面
                        //之前有验证码检查
                        driver.get(Comp_Url);
                        detail_comp(first_comp_name,driver,Type_comp);
                    }
                }

            }catch (Exception e){
                Thread.sleep(10000);
                log.info("获取公司搜索结果失败");
            }

        }catch(Exception e){
            log.error(e);
        }

    }

    //根据文件中的URL获得公司的详细信息
    private static void detail_comp(String first_comp_name,WebDriver driver,String Type_comp) throws InterruptedException {
        try{
            check_Verifycode(driver);
            driver.manage().timeouts().implicitlyWait(500, TimeUnit.MILLISECONDS);
            StringBuilder content = new StringBuilder();
            content.append(first_comp_name).append("/");

            //写入公司联系方式
            content.append(getComp_Contact(driver));

            if(!Type_comp.equals("事业单位")&!Type_comp.equals("基金会")&!Type_comp.equals("社会组织")) {
                String legalPerson = driver.findElement(By.cssSelector("div#_container_baseInfo>table>tbody .humancompany .name .link-click")).getAttribute("title");
                content.append(legalPerson).append("/");
            }

            WebElement table = driver.findElement(By.cssSelector("div#_container_baseInfo>table.table.-striped-col>tbody"));
            System.out.println("开始写入");

            //遍历table，将读取内容打印到TXT文本中
            List<WebElement> rows = table.findElements(By.tagName("tr"));
            for (WebElement row  : rows ) {

                List<WebElement> cols = row.findElements(By.tagName("td"));

                if (cols.size()>2){
                    WebElement col1 = cols.get(1);

                    WebElement col2 = cols.get(3);
                    content.append(col1.getText()).append("/").append(col2.getText()).append("/");
                }
                else{
                    content.append(cols.get(1).getText()).append("/");
                }
            }
            //写入文档
            if (Type_comp.equals("事业单位")){
                writeTxt(readProperties("info_inst_path"),content.toString());
            }else if (Type_comp.equals("社会组织")){
                writeTxt(readProperties("info_socialOrg_path"),content.toString());
            }else if (Type_comp.equals("基金会")){
                writeTxt(readProperties("info_found_path"),content.toString());
            }else{
            writeTxt(readProperties("info_comp_path"),content.toString());
            }
            System.out.println("写入成功");

        } catch (Exception e) {
            Thread.sleep(10000);
            log.error("未能准确定位公司信息"+e);
        }

    }

    private static StringBuilder getComp_Contact(WebDriver driver) {
        StringBuilder content = new StringBuilder();
        String phone = driver.findElement(By.cssSelector("div#company_web_top .box.-company-box .content .detail " +
                ".f0 .in-block.sup-ie-company-header-child-1>span:nth-child(2)")).getText();
        String email = driver.findElement(By.cssSelector("div#company_web_top .box.-company-box .content .detail " +
                ".f0 .in-block.sup-ie-company-header-child-2>span:nth-child(2)")).getText();
        String website;
        try {
            if (phone.equals("暂无信息")) {
                content.append("-").append("/");
            }else{
                content.append(phone).append("/");
            }
            if (email.equals("暂无信息")) {
                content.append("-").append("/");
            } else {
                content.append(email).append("/");
            }
            if (WebElementExist(driver, By.cssSelector("div#company_web_top .box.-company-box .content .detail " +
                    ".f0.clearfix.mb0 .in-block.sup-ie-company-header-child-1>.company-link"))) {
                website = driver.findElement(By.cssSelector("div#company_web_top .box.-company-box .content .detail " +
                        ".f0.clearfix.mb0 .in-block.sup-ie-company-header-child-1>.company-link")).getText();
            } else {
                website = "-";
            }
            content.append(website).append("/");
        }catch(Exception e){
            log.error("这里读取时出了问题"+e);
        }

        return content;
    }

    //检查是否需要登录
    private static void check_sign_in(WebDriver driver) throws InterruptedException {
        try {
            String signin_pop_up = "div#web-content .login";
            if (WebElementExist(driver, By.cssSelector(signin_pop_up))) {
                log.info("检查到需要登录");
                signin(driver);
            }
        }catch(Exception e){
            Thread.sleep(20000);
            log.error(e);
        }
    }

    //执行登录
    private static void signin(WebDriver driver){

        String account = readProperties("account");
        String password = readProperties("password");
        String password_login = ".loginmodule.collapse.in>.sign-in>.title-tab.text-center>div:nth-child(2)";

        try {
            driver.findElement(By.cssSelector(password_login)).click();
            driver.findElement(By.cssSelector("input#mobile.input.contactphone.js-live-search-phone")).sendKeys(account);
            driver.findElement(By.cssSelector("input#password.input.contactword.input-pwd")).sendKeys(password);
            driver.findElement(By.cssSelector(".modulein.modulein1.mobile_box.f-base.collapse.in .btn.-xl.btn-primary.-block")).click();
            Thread.sleep(5000);
        }catch(Exception e ){
            log.error("///   登录失败   ///"+e);
        }

    }

    //读取文件
    private static ArrayList<String> readTxt(String path){
        ArrayList<String> urls_comp = new ArrayList<String>();
        String line;
        try{
            InputStreamReader file = new InputStreamReader(new FileInputStream(path),"UTF-8");
            BufferedReader bufferedread = new BufferedReader(file);
            while((line = bufferedread.readLine())!= null){
                String[] name = line.split("\t");
                urls_comp.add(name[3]);
            }
            return urls_comp;

        } catch (Exception e) {
            log.error("读取文件失败"+e);
        }
        return null;
    }

    //写入文件
    private static void writeTxt(String path,String content ){
        File f = new File(path);
        boolean append = false;
        if (f.exists()){
            append = true;
        }
        try {
            //将写入转化为流的形式
            BufferedWriter bw = new BufferedWriter(new FileWriter(f, append));
            //一次写一行
            bw.write(content);
            bw.newLine();  //换行用

            //关闭流
            bw.close();

        }catch(Exception e){
            log.error("写入文件失败"+e);
        }
    }

    private static void write_last_name(String path,String content){
        try {
            File f = new File(path);
            //将写入转化为流的形式
            BufferedWriter bw = new BufferedWriter(new FileWriter(f, false));
            //一次写一行
            bw.write(content);
            bw.newLine();  //换行用
            //关闭流
            bw.close();
        }catch(Exception e){
            log.error("lastname文件写入失败"+e);
        }
    }

    //读取config内的配置
    public static String readProperties(String Keys){
        ReadProperties properties = new ReadProperties();
        return properties.getValue(Keys);
    }


    private static int random(int num){
        Random random = new Random();
        return (random.nextInt((num))+1);
    }

    //检查元素是否存在
    public static boolean WebElementExist(WebDriver driver, By selector){
        try {
            driver.findElement(selector);
            return true;
        }catch(Exception e){
            return false;
        }
    }

    //检查验证码是否弹出
    private static void check_Verifycode(WebDriver driver){
        try{
            String verifycode = "div.container>.container.mt74>.content>div:first-child";
            if (WebElementExist(driver, By.cssSelector(verifycode))){
                Thread.sleep(20000);
            }
        }catch(Exception e){
            log.error("checking verification code exception"+e);
        }
    }

}

//httpclient  发送参数  post  get
//代理ip
//spring boot 框架
