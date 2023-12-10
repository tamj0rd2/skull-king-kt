package com.tamj0rd2.webapp

import com.tamj0rd2.messaging.Message
import org.slf4j.Logger

fun Logger.receivedMessage(message: Message) =
    when (message) {
        is Message.AckFromClient,
        is Message.AckFromServer -> debug("got ack: {}", message.id)

        is Message.ToServer -> info("received: {}", message)
        is Message.ToClient -> debug("received: {}", message.id)
        is Message.Nack -> debug("received: {}", message)
        is Message.KeepAlive -> {}
    }

fun Logger.processedMessage(message: Message) =
    when (message) {
        is Message.AckFromClient,
        is Message.AckFromServer -> debug(">> completed << {}", message.id)

        is Message.ToServer -> debug("processed: {}", message.id)
        is Message.ToClient -> debug("processed: {}", message.id)
        is Message.Nack -> TODO()
        is Message.KeepAlive -> {}
    }

fun Logger.sending(message: Message) =
    when (message) {
        is Message.AckFromClient -> debug("acking: {}", message)
        is Message.AckFromServer -> {
            if (message.notifications.isNotEmpty()) info("acking: $message") else debug("acking: {}", message)
        }

        is Message.ToServer -> info("sending: $message")
        is Message.ToClient -> info("sending: $message")
        is Message.Nack -> info("sending: $message")
        is Message.KeepAlive -> {}
    }

fun Logger.awaitingAck(message: Message) =
    when (message) {
        is Message.AckFromClient,
        is Message.AckFromServer -> error("cannot await an ack of an ack")

        is Message.ToServer -> debug("awaiting ack: {}", message.id)
        is Message.ToClient -> debug("awaiting ack: {}", message.id)
        is Message.Nack -> error("cannot await an ack of a proessing failure")
        is Message.KeepAlive -> {}
    }

fun Logger.sentMessage(message: Message) =
    when (message) {
        is Message.AckFromClient,
        is Message.AckFromServer -> debug("sent ack: {}", message)

        is Message.ToServer -> debug("sent message: {}", message.id)
        is Message.ToClient -> debug("sent message: {}", message.id)
        is Message.Nack -> debug("sent processing failure: {}", message)
        is Message.KeepAlive -> {}
    }
