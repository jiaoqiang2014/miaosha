configuration
---
该配置文件说明如何在本地搭建起环境，包括启动 mysql、启动虚拟机、启动 redis。
## 一、Mysql启动
- 打开navicat 连接 127.0.0.1 即可。 
- 可以使用 http://www.localhost:8080/demo/db/get 测试

## 二、Redis 启动
redis 是使用 multipass 安装的虚拟机需要先启动虚拟机，然后在虚拟机中启动redis。

### 1、启动 multipass
```shell
multipass info -all
multipass shell XXX(host)
```
参考链接：http://www.manongjc.com/detail/21-gtzvxvzjtoaxswf.html

### 2、启动 redis
```shell
cd /usr/local/redis
redis-server redis.conf
redis-cli
auth 123456
```
可以使用 http://www.localhost:8080/demo/redis/get 测试

## 三、压测
### 1. 图形化压测

### 2. 命令行压测
首先，需要将jar包启动（参考`Spring Boot打jar包`）。然后，使用jmeter压测。
```shell
jmeter.sh -n -t /Users/jq/Desktop/goods_list.jmx -l result.jtl
```
如果OOM，可以使用下面命令设置更大的JVM堆栈内存。
```shell
JVM_ARGS="-Xms512m -Xmx5g" jmeter.sh -n -t /Users/jq/Desktop/goods_list.jmx -l result.jtl
```

## 四、Spring Boot打war包。
### 1. 添加依赖
```html
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>
</dependency>
```

### 2. 添加
```html
<build>
    <finalName>${project.artifactId}</finalName>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-war-plugin</artifactId>
            <configuration>
                <failOnMissingWebXml>false</failOnMissingWebXml>
            </configuration>
        </plugin>
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.1</version>
            <configuration>
                <fork>true</fork
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 3. 修改
```html
<groupId>org.imooc</groupId>
<artifactId>miaosha</artifactId>
<version>1.0-SNAPSHOT</version>
<packaging>war</packaging>
```
同时，把packaging中的jar修改为war。

### 4. 修改MainApplication类
```java
@SpringBootApplication
public class MainApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder){
        return builder.sources(MainApplication.class);
    }
}
```

### 5. brew 安装 tomcat@9（和java8匹配) \
Brew会默认安装在/opt/homebrew/Cellar/目录下。

### 6. 配置tomcat快速启动
```shell
alias tomcatstart=/opt/homebrew/Cellar/tomcat@9/9.0.60/libexec/bin/startup.sh
alias tomcatstop=/opt/homebrew/Cellar/tomcat@9/9.0.60/libexec/bin/shutdown.sh
```

### 7. 打成 war 包 
```shell
mvn clean package
```

在miaosha/target目录下生成miasma.war包。将miasma.war拷贝到 /opt/homebrew/Cellar/tomcat@9/9.0.60/libexec/webapps 目录下。

### 8. tomcatstart 启动tomcat。 \
输入http://www.localhost:8080测试tomcat是否搭建成功。输入 http://www.localhost:8080/miaosha/login/to_login， 查看登录界面，但目前不能登录，需要在/opt/homebrew/Cellar/tomcat@9/9.0.60/libexec/webapps/Root 目录下添加一些资源。

## 五、Spring Boot打jar包
### 1、添加依赖
```html
<build>
    <finalName>${project.artifactId}</finalName>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.1</version>
            <configuration>
                <fork>true</fork>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 2、修改
```html
<groupId>org.imooc</groupId>
<artifactId>miaosha</artifactId>
<version>1.0-SNAPSHOT</version>
<packaging>jar</packaging>
```


### 3、打成 jar 包
```shell
mvn clean package
```

在miaosha/target目录下生成miasma.jar包

### 4、运行jar，并输出到nohup文件。
```shell
nohup java -jar miaosha.jar &
```

### 5、测试
输入 http://www.localhost:8080/miaosha/login/to_login， 查看登录界面，并且进行压测。 \
输入 tail -f nohup.out 可以查看日志的末尾输出。

## 五、压测 redis
```shell
redis-benchmark -a 123456 -h 127.0.0.1 -p 6379 -c 100 -n 100000

redis-benchmark -a 123456 -h 127.0.0.1 -p 6379 -q -d 100

redis-benchmark -a 123456 -t set,lpush -n 100000 -q

# 只测试单条命令
redis-benchmark -a 123456 -n 100000 -q script load "redis.call('set','foo','bar')"
```

## 六、页面优化技术
- 页面缓存 + URL缓存 + 对象缓存 
- 页面静态化（不需要重复下载页面，只需要下载动态的），前后端分离
- 静态资源优化
- CDN优化

### 1、页面缓存
将 goods_list 页面的信息写入 redis，设置了60s的缓存时间。可以下命令测试：
```shell
keys GoodsKey:gl
get GoodsKey:gl
```

### 2、URL 缓存
和页面缓存大致一样，给 goods_detail 页面添加缓存，不同于 goods_list 页面的是 goods_detail 页面需要加用户的标号："1"（http://www.localhost:8080/goods/to_detail/1）。

### 3、对象缓存
此处的对象指的是用户对象，通过用户id将用户的信息写入缓存。

MiaoshaUserService 类中的 updatePassword 方法，如何更新数据库密码。

### 4、页面静态化
将数据存在浏览器中，主要技术有：AngularJS、Vue.js等
此处使用简单的。

服务端不直接返回html，只返回页面上动态的数据。在前端html页面中接受这些动态的数值即可。

#### 思考：如何验证客户端加载了浏览器的本地缓存而不是服务端的数据？\
304状态码表示服务端数据未改变，可直接使用客户端未过期的缓存。304状态码返回时不包含任何响应的主体部分。
请求首部包含`If-Modified-Since: Mon, 11 Apr 2022 10:07:49 GMT`，服务端会和资源的最近更新时间比较，确定是不是需要返回资源。不需要则返回304状态码，具体如下：
```html
HTTP/1.1 304
Last-Modified: Mon, 11 Apr 2022 10:07:49 GMT
Date: Mon, 11 Apr 2022 10:10:19 GMT
```
#### 进一步优化
上面虽然没有直接下载服务端的数据，但还是请求了一次服务端。通过在静态资源中添加设置静态资源的有效时间，不访问服务器，直接使用客户端的缓存。\
#### 验证
查看网页请求，发现响应如下：
```html
HTTP/1.1 200
Last-Modified: Mon, 11 Apr 2022 10:07:49 GMT
Content-Length: 4818
Accept-Ranges: bytes
Content-Type: text/html
Cache-Control: max-age=3600
Date: Mon, 11 Apr 2022 10:43:17 GMT
```
`Cache-Control: max-age=3600`字段表示该资源可以在3600ms内复用。

## 问题1：库存会被减成负值
#### 原因：两个人同时减库存时，调用的sql语句如下：
```java
@Update("update miaosha_goods set stock_count = stock_count - 1 where goods_id = #{goodsId}”)
```
当只有一个库存时会降为-1，此时可以在sql中加库存大于0的判断如下：
```java
@Update("update miaosha_goods set stock_count = stock_count - 1 where goods_id = #{goodsId} and stock_count > 0”)`）
```

## 问题2：一个用户购买了两个同一个商品。
#### 原因：库存为10，同一个用户同时发出两个请求，同时进入了判断库存等方法，导致一个用户买到两个商品。
解决思路：购买流程是：减库存 -> 下订单，下订单的时候有个订单表，有用户id和商品id，此处在订单表中给这两个字段建立联合唯一索引。这样创建订单时，如此该用户存在一个订单，再下另一个订单时就会出现重复。注意：在创建订单的函数前加`@Transactional`。
如果只给用户id建立唯一索引可以不？不行，允许用户秒杀其他商品。

### 5、静态资源优化
1. JS/CSS 压缩，减少流浪；
2. 多个 JS/CSS 组合
3. Tengine 在nginx基础上开发的
CDN:内容分发网络。 

## 七、秒杀接口优化
### 方案：
- 把秒杀商品信息加载到redis，减少mysql的访问。
- 内存标记，减少一次redis查商品库存的访问：使用map（goodsId -> boolean）， false表示还有库存，true表示没有库存，则秒杀结束。接下来的关于goodsId的请求不在访问redis，直接返回。
- 通过在redis中预减库存，当库存不足，直接返回，不需要进一步查询Redis中的订单信息，以判断是否秒杀成功。减少了redis的访问。
- 使用rabbitmq实现异步下单，达到削峰的作用。
### 秒杀流程：
- 系统初始化，把商品库存数量加载到redis。
- 收到请求，内存标记，减少redis访问：
- redis预减库存，如果库存不足，设置该goodsId的map值为true，然后直接返回。
- 请求入队，立即返回排队中。
- 请求出队，生成订单，减少库存。
- 客户端轮询，是否秒杀成功。

#### 环境安装
安装 RabbitMQ。
```shell
sudo apt-get install erlang
erl
```
以上命令能正常输出说明`erlang`安装成功。接下来安装、启动并验证`rabbitmq`是否在监听5672端口。
```shell
sudo apt-get install rabbitmq-server
sudo rabbitmq-server
netstat -nap | grep 5672
```
关闭`rabbitmq`。
```shell
sudo rabbitmqctl stop
```

#### 设置一个 rabbitmq 的用户名和密码，默认存在一个用户 guest，密码为 guest。
```shell
rabbitmqctl add_user YOUR_USERNAME YOUR_PASSWORD
rabbitmqctl set_user_tags YOUR_USERNAME administrator
rabbitmqctl set_permissions -p / YOUR_USERNAME ".*" ".*" ".*"
```

### 扩展
- Nginx水平扩展。
- 分库分表