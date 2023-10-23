import {Command} from "./GameEvents";
import {PlayerId} from "./Constants";

export const socket = new WebSocket(INITIAL_STATE.endpoint)

export enum MessageType {
    AckFromClient = "Message$Ack$FromClient",
    AckFromServer = "Message$Ack$FromServer",
    Nack = "Message$Nack",
    ToClient = "Message$ToClient",
    ToServer = "Message$ToServer"
}

export interface Message {
    type: MessageType
    [key: string]: any
}

export function sendCommand(command: Command) {
    socket.send(JSON.stringify({ type: MessageType.ToServer, command }))
}

let playerId: PlayerId = "unidentified"

export function getPlayerId(): PlayerId {
    return playerId
}

export function setPlayerId(newId: PlayerId) {
    playerId = newId
}

socket.addEventListener("error", (event) => console.error(event))
socket.addEventListener("open", () => {
    console.log("connected to ws")

    // TODO: this needs to move elsewhere
    // sendCommand({ type: CommandType.JoinGame, playerId: INITIAL_STATE.playerId })
})

socket.addEventListener("message", (event) => {
    let data = JSON.parse(event.data);
    console.log(`${getPlayerId()} received ${event.data}`, data)
})
socket.addEventListener("close", (event) => console.warn(`disconnected from ws - ${event.reason}`, event))
