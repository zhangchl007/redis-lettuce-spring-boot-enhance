# Redis Lettuce client connection issue

(Redis-Client) https://github.com/lettuce-io/lettuce-core/issues/2082


Why KeepAlive doesn’t work?

```
Because the priority of the retransmission packet is higher than that of keepalive, before reaching the keepalive stage, it will continue to retransmit until it is reconnected.

```
Why timeout of tcp retransmission almost last 16 minutes?

```
net.ipv4.tcp_retries2=15   #### This value influences the timeout of an alive TCP connection,
	when RTO retransmissions remain unacknowledged.
	Given a value of N, a hypothetical TCP connection following
	exponential backoff with an initial RTO of TCP_RTO_MIN would
	retransmit N times before killing the connection at the (N+1)th RTO.

	The default value of 15 yields a hypothetical timeout of 924.6

```
Why TCP_USER_TIMEOUT help resolve this issue?

```
It sets the maximum amount of time that transmitted data may remain unacknowledged before the kernel forcefully closes the connection. On its own, it doesn't do much in the case of idle connections. The sockets will remain ESTABLISHED even if the connectivity is dropped. However, this socket option does change the semantics of TCP keepalives. The tcp(7) manpage is somewhat confusing:
Moreover, when used with the TCP keepalive (SO_KEEPALIVE) option, TCP_USER_TIMEOUT will override keepalive to determine when to close a connection due to keepalive failure.

```
What TCP_USER_TIMEOUT value?

```
We suggestion 100s , the tcp retransmission will being lasted around 2 minutes

```

## Enhance epoll code:

```
bootstrap.option(EpollChannelOption.TCP_KEEPIDLE, 15);
                bootstrap.option(EpollChannelOption.TCP_KEEPINTVL, 5);
                bootstrap.option(EpollChannelOption.TCP_KEEPCNT, 3);
                bootstrap.option(EpollChannelOption.TCP_USER_TIMEOUT, 100000);
                bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

<dependency>
      <groupId>io.netty</groupId>
      <artifactId>netty-transport-native-epoll</artifactId>
      <version>${netty.version}</version>
      <classifier>linux-x86_64</classifier>
/dependency>

```
## Requirements
* Java 8
* Apache Maven 3.5.0 or higher

## How to Run

- Clone the project
- Configure Redis password in application.yml
- Build the project  
```
mvn clean install
```
- Run the application
```
java -jar target/redis-0.0.1-SNAPSHOT.jar
```
- Make sure your redis-server is up and running
- Use the postman collection located in /src/main/resources directory to test the application.

### Reference Documentation

referene repo:  http://www.rajith.me/2020/02/using-redis-with-spring-boot.html
For further reference, please consider the following sections:

(Redis-Client) https://lettuce.io/# redis-lettuce-spring-boot-enhance
