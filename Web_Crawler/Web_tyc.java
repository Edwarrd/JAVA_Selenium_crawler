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



public class Web_tyc {

    public static Logger log = LogManager.getLogger(Web_tyc.class.getName());

    public static void main(String[] args){

        //****create chromedriverservice to monitor chormedriver****
        ChromeDriverService service = new ChromeDriverService.Builder()
                .usingDriverExecutable(new File("src/main/resources/res/chromedriver.exe"))
                .usingAnyFreePort().build();


        try {
            log.info("************     Start ChromeDriverService        **************");
            service.start();
            try {
                //********read target file**********
                String file_path = readProperties("file_path");//config file
                ArrayList<String> names = readTxt(file_path);//company name file

                log.info("************     start WebDriver     *************");
                //*******setting proxy***********
//                WebDriver driver = new RemoteWebDriver(service.getUrl(),
//                        new ChromeOptions().addArguments("--proxy-server=http://" + readProperties("ip")));

                ChromeOptions options = new ChromeOptions();
                
                //set profile of chromedriver
                //options.addArguments("user-data-dir="+user data path of chrome);
                WebDriver driver = new RemoteWebDriver(service.getUrl(),options);

                String URL = readProperties("url");//target website(tianyancha)
                driver.get(URL);
                //wait for all elements loading
                driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);

                //login
                driver.findElement(By.cssSelector("selector of login page")).click();
                signin(driver);

                
                //read names from the file and start crawler
                if(names!= null){
                    for (String name:names){
                            search(driver, name);
                            write_last_name(readProperties("recording_file_path"), name);
                    }
                }


                driver.close();
                log.info("******************    close WebDriver       *********************");
            } catch (Exception e) {
                log.error(" fail to read target file   "+e);
            }
        } catch (Exception e) {
            log.error("fail to start ChromeService  "+e);
        }
        finally {
            //program finish close chromeDriverService
            log.info("**********     Close ChromeDriverService         ***********");
            service.stop();
        }
    }

    //search based on company name and write results in file
    private static void search(WebDriver driver, String name){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try{
            //enter company name in search bar
            WebElement searchBar = driver.findElement(By.cssSelector(".js-live-search-auto"));
            searchBar.clear();
            searchBar.sendKeys(name);
            searchBar.sendKeys(Keys.ENTER);
            driver.manage().timeouts().implicitlyWait(500, TimeUnit.MILLISECONDS);

            //*****check verification status
            check_Verifycode(driver);

            try{
                //****retry if there is no result
                //sometimes due to the internet condition, result can not be found
                WebElement first_comp;
                    if (WebElementExist(driver,By.cssSelector(".result-list.no-result"))){
                        for (int i=0;i<random(3);i++){
                            driver.navigate().refresh();
                            if (!WebElementExist(driver,By.cssSelector(".result-list.no-result"))){
                                break;
                            }
                        }
                    }
                    //**choose the first company
                if (WebElementExist(driver,By.cssSelector(".result-list.sv-search-container>div:first-child .content .header .name"))) {
                    first_comp = driver.findElement(By.cssSelector(".result-list.sv-search-container>div:first-child .content .header .name"));

                    String first_comp_name = first_comp.getText();
                    //compare the result name and search name
                    System.out.println(df.format(System.currentTimeMillis())+"the search name is ：" + first_comp_name + "； and result is：" + name);
                    if (first_comp_name.equals(name)) {
                        System.out.println("name match, write into the file");
                        String Comp_Url = first_comp.getAttribute("href");
                        String Type_comp = driver.findElement(By.cssSelector(".result-list.sv-search-container>div:first-child .content .tag-list .tag-common.-primary.-new")).getText();
                        driver.get(Comp_Url);
                        detail_comp(first_comp_name,driver,Type_comp);
                    }
                }

            }catch (Exception e){
                Thread.sleep(10000);
                log.info("fail to get result");
            }

        }catch(Exception e){
            log.error(e);
        }

    }

    //get the detail information of company
    private static void detail_comp(String first_comp_name,WebDriver driver,String Type_comp) throws InterruptedException {
        try{
            check_Verifycode(driver);
            driver.manage().timeouts().implicitlyWait(500, TimeUnit.MILLISECONDS);
            StringBuilder content = new StringBuilder();
            content.append(first_comp_name).append("/");

            //get the contact information
            content.append(getComp_Contact(driver));

            if(!Type_comp.equals("事业单位")&!Type_comp.equals("基金会")&!Type_comp.equals("社会组织")) {
                String legalPerson = driver.findElement(By.cssSelector("div#_container_baseInfo>table>tbody .humancompany .name .link-click")).getAttribute("title");
                content.append(legalPerson).append("/");
            }

            WebElement table = driver.findElement(By.cssSelector("div#_container_baseInfo>table.table.-striped-col>tbody"));
                
          
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
            
            if (Type_comp.equals("事业单位")){
                writeTxt(readProperties("info_inst_path"),content.toString());
            }else if (Type_comp.equals("社会组织")){
                writeTxt(readProperties("info_socialOrg_path"),content.toString());
            }else if (Type_comp.equals("基金会")){
                writeTxt(readProperties("info_found_path"),content.toString());
            }else{
            writeTxt(readProperties("info_comp_path"),content.toString());
            }
            System.out.println("write successfully");

        } catch (Exception e) {
            Thread.sleep(10000);
            log.error("fail to get detail infomation"+e);
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
            log.error("fail to get contact infomation"+e);
        }

        return content;
    }

    
    //abandoned method 
    //was used to check the login status 
    private static void check_sign_in(WebDriver driver) throws InterruptedException {
        try {
            String signin_pop_up = "div#web-content .login";
            if (WebElementExist(driver, By.cssSelector(signin_pop_up))) {
                log.info("need to login");
                signin(driver);
            }
        }catch(Exception e){
            Thread.sleep(20000);
            log.error(e);
        }
    }

    //abandoned method
    //was used to login
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
            log.error("///   fail to login   ///"+e);
        }

    }

    //read file 
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
            log.error("fail to read file"+e);
        }
        return null;
    }

    //write file 
    private static void writeTxt(String path,String content ){
        File f = new File(path);
        boolean append = false;
        if (f.exists()){
            append = true;
        }
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(f, append));
            bw.write(content);
            bw.newLine();
            bw.close();

        }catch(Exception e){
            log.error("fail to write file"+e);
        }
    }

    
    //abandoned method
    //was used to located position last read
    private static void write_last_name(String path,String content){
        try {
            File f = new File(path);
            BufferedWriter bw = new BufferedWriter(new FileWriter(f, false));
            bw.write(content);
            bw.newLine(); 
            bw.close();
        }catch(Exception e){
            log.error("fail to write lastname"+e);
        }
    }

    //read config file
    public static String readProperties(String Keys){
        ReadProperties properties = new ReadProperties();
        return properties.getValue(Keys);
    }


    //anadoned method
    //was used to monitor human action
    //caused the program too slow
    private static int random(int num){
        Random random = new Random();
        return (random.nextInt((num))+1);
    }

    public static boolean WebElementExist(WebDriver driver, By selector){
        try {
            driver.findElement(selector);
            return true;
        }catch(Exception e){
            return false;
        }
    }

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
