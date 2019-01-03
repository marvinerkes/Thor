/*
 * Copyright (c) 2017 Marvin Erkes
 *
 * This file is part of Thor.
 *
 * Thor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.progme.thor.client.pub.Publisher;
import de.progme.thor.client.pub.PublisherFactory;
import de.progme.thor.client.sub.Subscriber;
import de.progme.thor.client.sub.SubscriberFactory;
import org.json.JSONObject;

/**
 * Created by Marvin Erkes on 25.03.2017.
 */
public class PublisherTest {

    //public static final String HOST = "192.168.2.102";
    public static final String HOST = "localhost";
    public static final int CLIENTS = 20;
    public static final int MESSAGES_PER_CLIENT = 100;
    public static final int TOTAL_MESSAGES = CLIENTS * MESSAGES_PER_CLIENT;

    public static void main(String[] args) {

        Subscriber subscriber = SubscriberFactory.create(HOST, 6000, "some-subscriber");
        subscriber.subscribe(TestChannelHandler.class);
        //subscriber.subscribe(TestGsonChannelHandler.class);
        //subscriber.subscribeMulti(BackendMultiChannelHandler.class);

/*        CountDownLatch countDownLatch = new CountDownLatch(CLIENTS);

        for (int i = 0; i < CLIENTS; i++) {
            new Thread(() -> {

                Publisher publisher = PublisherFactory.create(HOST, 1337);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("foo", "bar");

                for (int j = 0; j < MESSAGES_PER_CLIENT; j++) {
                    publisher.publish("test", jsonObject);

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                publisher.disconnect();

                countDownLatch.countDown();
            }).start();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Test finished successfully! " + TestChannelHandler.PACKETS +  "/" + TOTAL_MESSAGES);

        subscriber.disconnect();*/

        Publisher publisher = PublisherFactory.create(HOST, 6000);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("foo", "bar");
        publisher.publish("test", jsonObject);

        publisher.publishAll("test", jsonObject, new JSONObject().put("foo", "second"));

        publisher.publishAll("test", "some-subscriber", jsonObject, new JSONObject().put("foo", "second"));

        publisher.publishAll("gson", new FooBar("bar"), new FooBar("bar2"));

        publisher.publishAll("gson", "some-subscriber", new FooBar("bar"), new FooBar("bar2"));

        JSONObject backendJson = new JSONObject();
        backendJson.put("role", "update");
        backendJson.put("ping", 5);
        publisher.async().publish("backend", backendJson);

        for (int i = 0; i < 20; i++) {
            publisher.publish("test", new FooBar("bar" + i));

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        subscriber.disconnect();
        publisher.disconnect();
    }
}
