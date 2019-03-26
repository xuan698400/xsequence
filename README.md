# 分布式序列号生成组件

#### 项目介绍
微服务时代，我们需要生产一个连续的友好的序列号，例如订单号等。变得比较麻烦。<br/>
这里我提供了两种业界常用的解决方案来实现这个分布式序列号生成组件。<br/>
（1）使用集中式存储功能取步长进行分配。目前支持数据库（Mysql）、Redis<br/>
（2）使用雪花算法获取连续序列号，保证多服务器集群不重复<br/>
组件存在的目的就是屏蔽序列号底层实现，支持多样化的算法。让用户开箱即用。方便开发。<br/>

#### 项目结构

1. xsequence-core：核心代码
2. xsequence-test：测试代码
3. doc：需要存放一些测试数据和文档

#### Maven支持
<pre><code>
&lt;dependency>
    &lt;groupId>com.xuanner&lt;/groupId>
    &lt;artifactId>xsequence-core&lt;/artifactId>
    &lt;version>1.6&lt;/version>
&lt;/dependency>
</pre></code>

#### 升级日志
v1.0<br/>
新加特性：支持DB方式生成序列号<br/>
使用文档：https://my.oschina.net/u/1271235/blog/1808103<br/>
更新时间：2018/05/07<br/>

v1.1<br/>
新加特性：支持Redis方式生成序列号<br/>
使用文档：https://my.oschina.net/u/1271235/blog/1809437<br/>
更新时间：2018/05/09<br/>

v1.2<br/>
新加特性：支持雪花算法方式生成序列号<br/>
使用文档：https://my.oschina.net/u/1271235/blog/1812305<br/>
更新时间：2018/05/14<br/>

v1.3<br/>
新加特性：<br/>
（1）友好API的封装，让用户尽量少感知底层实现。<br/>
（2）对3种实现进行jmh基准测试，并做了结果对比。<br/>
（3）优化了雪花算法，支持线程安全。<br/>
更新时间：2018/05/31<br/>

v1.4<br/>
没有改任何东西，只是之前的版本在maven中无法下载，1.4之后就可以正常下载了
资讯地址：https://www.oschina.net/news/96840/xsequence-1-4-released

v1.5<br/>
新增uuid生成工具类：UUIDUtils.uuid()

v1.6<br/>
支持指定起始位置，例如需要60000起，那么就设置stepStart=60000，那么序列号就会从60000开始分配

#### 性能对比
具体的测试报告可以在doc目录下的jmh文件夹里面找，这里做一个简单的介绍<br/>

数据库和redis部署主机配置：<br/>
CPU：1核<br/>
内存：1GB<br/>
操作系统：CentOS 6.8 64位<br/>
使用宽带：1Mbps<br/>


| 实现算法 | ops/s（每秒产生序列号） | 备注说明 | 
| - | - | - | 
| 使用DB获取区间 | 314754.083 ± 1089932.384  ops/s| Mysql是单机部署，具体连接参数看测试包，步长1000 | 
| 使用Redis获取区间 | 244326.027 ± 518524.654  ops/s |  Redis是单机部署，步长1000 | 
| 使用雪花算法获取 | 4076501.612 ± 44955.567  ops/s | 缺点就是长度比较长 |
| 使用UUID工具类生成唯一编号 | 3368986.174 ± 724159.973  ops/s | 缺点就是长度32位，无序 |

总结：
上面取步长生成序列号的性能和具体DB、Redis的性能关系很大，而且和步长设置大小也有很大关系。
这里我用了比较差的机器配置。DB和Redis也都是单机比较差的。在这样的情况下，获取序列号的性能也还可以。
其中雪花算法的性能和机器没啥关系了，他的上限也没有可利用空间。使用Redis方式性能的提升空间最大。

#### 简单使用
（1）使用DB获取区间方式<br/>
<pre><code>
public class DbTest_Api extends BaseTest {

    private Sequence sequence;

    @Before
    public void setup() {
        //数据源
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("xxx");
        dataSource.setUsername("xxx");
        dataSource.setPassword("xxx");
        dataSource.setMaxActive(300);
        dataSource.setMinIdle(50);
        dataSource.setInitialSize(2);
        dataSource.setMaxWait(500);

        /**
         * 参数说明如下：
         * dataSource：数据库的数据源
         * bizName：具体某中业务的序列号
         * step：[可选] 默认1000，即每次取redis获取步长值，根据具体业务吞吐量来设置，越大性能越好，但是序列号断层的风险也就越大
         */
        sequence = DbSeqBuilder.create().dataSource(dataSource).bizName("userId").build();
    }

    @Test
    public void test() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            System.out.println("++++++++++id:" + sequence.nextValue());
        }
        System.out.println("interval time:" + (System.currentTimeMillis() - start));
    }
}
</pre></code>

（2）使用Redis获取区间方式<br/>
<pre><code>
/**
 * 参数说明如下：
 * ip：redis连接ip
 * port：redis连接port
 * auth：如果redis设置了密码权限需要设置，没有就可以不用设置
 * bizName：具体某中业务的序列号
 * step：[可选] 默认1000，即每次取redis获取步长值，根据具体业务吞吐量来设置，越大性能越好，但是序列号断层的风险也就越大
 */
sequence = RedisSeqBuilder.create().ip("xxx").port(6379).auth("xxx").step(1000).bizName(
                "userId").build();
</pre></code>

（3）使用雪花算法方式<br/>
<pre><code>
 /**
 * 参数说明如下：
 * datacenterId: 一般可以设置机房标志，值只能设置[0-31]之间
 * workerId: 一般设置主机编号，值只能设置[0-31]之间
 * 只用这来那个值保证服务器唯一就可以
 */
sequence = SnowflakeSeqBuilder.create().datacenterId(1).workerId(2).build();
</pre></code>

（4）UUID工具类使用<br/>
<pre><code>
UUIDUtils.uuid()
</pre></code>

#### 联系方式
1. 姓名：徐安
2. 邮箱:javaandswing@163.com
3. QQ：349309307
4. 个人博客：xuanner.com
5. QQ交流群：813221731