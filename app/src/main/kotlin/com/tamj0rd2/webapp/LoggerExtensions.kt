package com.tamj0rd2.webapp

import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.messaging.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun PlayerId.logger(context: String) = LoggerFactory.getLogger("$this:$context")

fun Logger.receivedMessage(message: Message) =
    when (message) {
        is Message.AcceptanceFromClient,
        is Message.AcceptanceFromServer,
        -> debug("got ack: {}", message.id)

        is Message.ToServer -> info("received: {}", message)
        is Message.ToClient -> debug("received: {}", message.id)
        is Message.Rejection -> debug("received: {}", message)
        is Message.KeepAlive -> {}
    }

fun Logger.processedMessage(message: Message) =
    when (message) {
        is Message.AcceptanceFromClient,
        is Message.AcceptanceFromServer,
        -> debug(">> completed << {}", message.id)

        is Message.ToServer -> debug("processed: {}", message.id)
        is Message.ToClient -> debug("processed: {}", message.id)
        is Message.Rejection -> TODO()
        is Message.KeepAlive -> {}
    }

fun Logger.sending(message: Message) =
    when (message) {
        is Message.AcceptanceFromClient,
        is Message.AcceptanceFromServer,
        -> debug("acking: {}", message)

        is Message.ToServer,
        is Message.ToClient,
        is Message.Rejection,
        -> info("sending: $message")

        is Message.KeepAlive -> {}
    }

fun Logger.awaitingAck(message: Message) =
    when (message) {
        is Message.AcceptanceFromClient,
        is Message.AcceptanceFromServer,
        -> error("cannot await an ack of an ack")

        is Message.ToServer -> debug("awaiting ack: {}", message.id)
        is Message.ToClient -> debug("awaiting ack: {}", message.id)
        is Message.Rejection -> error("cannot await an ack of a proessing failure")
        is Message.KeepAlive -> {}
    }

fun Logger.sentMessage(message: Message) =
    when (message) {
        is Message.AcceptanceFromClient,
        is Message.AcceptanceFromServer,
        -> debug("sent ack: {}", message)

        is Message.ToServer -> debug("sent message: {}", message.id)
        is Message.ToClient -> debug("sent message: {}", message.id)
        is Message.Rejection -> debug("sent processing failure: {}", message)
        is Message.KeepAlive -> {}
    }
