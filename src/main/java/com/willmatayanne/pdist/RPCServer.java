package com.willmatayanne.pdist;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.*;

public class RPCServer {

    private static String RPC_QUEUE  = "row_rpc";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(RPC_QUEUE, false, false, false, null);
        channel.queuePurge(RPC_QUEUE);

        channel.basicQos(1);

        System.out.println("Aguardando requiscoes do RPC .... \n");

        Object monitor = new Object();
        DeliverCallback callback = (consumerTag, delivery) -> {
            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(delivery.getProperties().getCorrelationId())
                    .build();

            try {
                String deliveryMessage = new String(delivery.getBody(), "UTF-8");

                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps,
                        String.format("Olá , %s", deliveryMessage).getBytes());
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                System.out.println("Recebendo requiscao com ID " + delivery.getProperties().getCorrelationId());
            } catch (Exception err) {
                System.out.println(err);
            }
        };

        channel.basicConsume(RPC_QUEUE, false, callback, consumerTag -> {});

        while (true) {
            synchronized (monitor) {
                try {
                    monitor.wait();
                } catch (InterruptedException err) {
                    err.printStackTrace();
                }
            }
        }
    }
}
