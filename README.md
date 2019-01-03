# Thor
Thor is a robust and lightweight Java Pub/Sub library and in-memory key-value cache which uses JSON.

I have started this project to learn myself how Pub/Sub libraries can work and it has turned out that Thor works really well,
therefore I have decided to publish it here on GitHub.

Thor now has also memory snapshot support to create a backup of cached data or to use it as a "persistent" memory database.

# Features

- in-memory key-value cache (based on json, so class serialization possible)
- cache memory snapshots
- channels
- channel handler (json object and custom object)
- key-value based handler method invocation
- subscriber may have custom names
- publish to specific subscriber
- custom object serialization with [gson] (https://github.com/google/gson)
- clean API
- extensive API
- low cpu and memory consumption
- lightweight
- scalable
- simple JSON
- easy to implement in other languages (PHP publisher example at the bottom)
- async API support
- full multi-core utilization and configurable number of threads
- combine multiple Thor server to a cluster
- cluster failover with auto reconnect and sync
- simple but powerful NIO implementation
- thousands of publisher and subscriber concurrently
- hundreds of new connections per second

# Installation

- Install [Maven 3](http://maven.apache.org/download.cgi)
- Clone/Download this repo
- Install it with: ```mvn clean install```

**Maven dependencies**

_Client:_

```xml
<dependency>
    <groupId>de.progme</groupId>
    <artifactId>thor-client</artifactId>
    <version>3.0.2-SNAPSHOT</version>
</dependency>
```

# Server start with the CLI

_General syntax:_

```
java -jar thor-server.jar -h <Host> -p <Port> -b <Backlog> -t <Threads> [-c IP:Port IP:Port] [-d]
```

_Without debugging and without cluster setup:_

```
java -jar thor-server.jar -h localhost -p 1337 -b 100 -t 4
```

_With debugging and without cluster setup:_

```
java -jar thor-server.jar -h localhost -p 1337 -b 100 -t 4 -d
```

_With debugging and with cluster setup:_

```
java -jar thor-server.jar -h localhost -p 1337 -b 100 -t 4 -c localhost:1338 -d
```

_Set the check (ci) or snapshot (si) interval:_

```
// Both are in seconds
// This will check the cache for expired entries every 10 minutes
// and will take a memory snapshot (backup) every hour
java -jar thor-server.jar -h localhost -p 1337 -b 100 -t 4 -c localhost:1338 -d -ci 600 -si 3600
```

# API examples

_Cache:_
```java
PubSubCache cache = PubSubCacheFactory.create("localhost", 1337);
// To create a pub sub cache from a cluster
//PubSubCache cache = PubSubCacheFactory.create(Arrays.asList(new ClusterServer("localhost", 1337), new ClusterServer("localhost", 1338)));

cache.put("json", new JSONObject().put("foo", "bar"));
cache.get("json", jsonObject -> System.out.println("Foo: " + jsonObject.get("foo")));
cache.async().get("json", jsonObject -> System.out.println("Foo: " + jsonObject.get("foo")));

// The class needs to implement the 'Cacheable' interface to be serialized to json
// Key will expire in 5 seconds
cache.put("test", new FooBar("Some text"), 5);
cache.get("test", json -> System.out.println("FooBar class: " + json.toString()));

// Gets the time in seconds how long the key lives until it's expired
cache.expire("test", System.out::println);

try {
	Thread.sleep(1000);
} catch (InterruptedException e) {
	e.printStackTrace();
}

cache.expire("test", System.out::println);

// Gets the class instance of the given class from the key
cache.getClass("test", fooBar -> System.out.println("Foo: " + fooBar.getFoo()), FooBar.class);
// The same as above but without lambda
/*cache.getClass("test", new Consumer<FooBar>() {

	@Override
	public void accept(FooBar fooBar) {

		System.out.println(fooBar);
	}
}, FooBar.class);*/

// Removes the key from the cache
cache.remove("json");

// Disconnects the cache client
cache.disconnect();
```

_Publisher:_
```java
Publisher publisher = PublisherFactory.create("localhost", 1337);
// To create a publisher from a cluster
//Publisher publisher = PublisherFactory.create(Arrays.asList(new ClusterServer("localhost", 1337), new ClusterServer("localhost", 1338)));

JSONObject fooBar = new JSONObject();
fooBar.put("foo", "bar");
publisher.publish("test", fooBar);

// Both will work
publisher.publish("gson", fooBar);
publisher.publish("gson", new FooBar("bar"));

JSONObject backendJson = new JSONObject();
backendJson.put("role", "update");
backendJson.put("ping", 5);
publisher.publish("backend", backendJson);

// Publish to a channel and a specific subscriber
publisher.publish("test", "some-subscriber", fooBar);

// Publish multiple json objects to a channel
publisher.publishAll("test", jsonObject, new JSONObject().put("foo", "second"));

// Publish multiple json objects to a channel and a specific subscriber
publisher.publishAll("test", "some-subscriber", jsonObject, new JSONObject().put("foo", "second"));

// Publish multiple objects to a channel
publisher.publishAll("gson", new FooBar("bar"), new FooBar("bar2"));

// Publish multiple objects to a channel and a specific subscriber
publisher.publishAll("gson", "some-subscriber", new FooBar("bar"), new FooBar("bar2"));

// You need to publish something async because you don't want to block your main thread perhaps?
// Here you go
AsyncPublisher asyncPublisher = publisher.async();
asyncPublisher.publish("test", fooBar);

// True because we want to force the disconnect so that it will not try to reconnect to the cluster
publisher.disconnect(true);
// Is the same as
//publisher.disconnect();
```

_Subscriber:_
```java
Subscriber subscriber = SubscriberFactory.create("localhost", 1337);
// To create a subscriber from a cluster
//Subscriber subscriber = SubscriberFactory.create(Arrays.asList(new ClusterServer("localhost", 1337), new ClusterServer("localhost", 1338)), "some-subscriber");
subscriber.subscribe(TestChannelHandler.class);
subscriber.subscribeMulti(BackendMultiChannelHandler.class);
subscriber.subscribe(GsonChannelHandler.class);

// True because we want to force the disconnect so that it will not try to reconnect to the cluster
subscriber.disconnect(true);
// Is the same as
//subscriber.disconnect();
```

_Subscriber with defined name:_
```java
Subscriber subscriber = SubscriberFactory.create("localhost", 1337, "some-subscriber");
subscriber.subscribe(TestChannelHandler.class);
```

_TestChannelHandler:_
```java
@Channel("test")
public class TestChannelHandler extends ChannelHandler<JSONObject> {

    @Override
    public void onMessage(String channel, JSONObject message) {

        System.out.println("TestChannelHandler: foo=" + message.get("foo"));
    }
}
```

_BackendMultiChannelHandler:_
```java
@Channel("backend")
public class BackendMultiChannelHandler {

    @Key("role")
    @Value("update")
    public void onBackendRoleUpdate(JSONObject jsonObject) {

		// Keep in mind that the key (here "role") will be removed before invocation
        System.out.println("BMCH[role=update]: ping=" + jsonObject.getInt("ping"));
    }
    
    @Key("role")
    @Value("delete")
    public void onBackendRoleDelete(FooBar fooBar) {

        System.out.println("FooBar[role=delete]: " + fooBar.toString());
    }
}
```

_GsonChannelHandler:_
```java
@Channel("gson")
public class GsonChannelHandler extends ChannelHandler<FooBar> {

    @Override
    public void onMessage(String channel, FooBar fooBar) {

        System.out.println("FooBar class: " + fooBar.toString());
    }
}
```

_The simple FooBar class:_
```java
public class FooBar {

	private String foo;

    public FooBar(String foo) {

        this.foo = foo;
    }

	public String getFoo() {

        return foo;
    }

    @Override
    public String toString() {

        return "FooBar{" +
                "foo='" + foo + '\'' +
                '}';
    }
}
```

# PHP example

_ThorPublisher:_
```php
<?php

class ThorPublisher
{

    /**
     * Host address.
     *
     * @var string The host address.
     */
    private $host;

    /**
     * Host port.
     *
     * @var int The host port.
     */
    private $port;

    /**
     * Socket instance.
     *
     * @var resource The socket instance.
     */
    private $socket;

    /**
     * ThorPublisher constructor.
     */
    /**
     * ThorPublisher constructor.
     *
     * @param string $host The host address to connect to.
     * @param int $port The host port to connect to.
     */
    public function __construct($host = "localhost", $port = 1337)
    {
        $this->host = $host;
        $this->port = $port;

        $this->socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);

        $this->connect();
    }

    /**
     * Connects the internal socket.
     */
    private function connect() {

        socket_connect($this->socket, $this->host, $this->port);
    }

    /**
     * Publish a message to a given channel.
     *
     * @param ($channel) The channel.
     * @param ($array) The message as an array which will be json encoded.
     */
    public function publish($channel, $array) {

        $array['op'] = 2;
        $array['ch'] = $channel;

        $json = json_encode($array);
        $jsonLength = strlen($json);
        $length = pack('N', $jsonLength);

        socket_write($this->socket, $length . $json, $jsonLength + 4);
    }

    /**
     * Closes the publisher.
     */
    public function close() {

        socket_close($this->socket);
    }
}
```

_Example usage:_
```php
<?php

include("ThorPublisher.php");

$client = new ThorPublisher("192.168.2.102", 6000);

$client->publish("test", array("foo" => "bar1"));
$client->publish("test", array("foo" => "bar2"));

$client->close();
```

### License

Licensed under the GNU General Public License, Version 3.0.
