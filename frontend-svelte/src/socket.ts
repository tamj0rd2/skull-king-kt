import {derived, readable} from "svelte/store";
import {Message, PlayerCommand, type PlayerGameState, type PlayerId} from "../generated_types";
import {v4 as uuidv4} from "uuid";

declare global {
    const INITIAL_STATE: {
        endpoint: string
        ackTimeoutMs: number
    }
}

interface WsClient {
    sendCommand: (command: PlayerCommand) => void
    state: PlayerGameState
}

const ws = readable<WebSocket>(
    undefined,
    set => {
        const socket = new WebSocket(INITIAL_STATE.endpoint)

        socket.onopen = () => console.log("connected to ws")
        socket.onclose = () => console.log("disconnected from ws")
        socket.onerror = (event) => {
            console.error("WebSocket error observed:", event)
            socket.close()
        }

        set(socket)
        return () => socket.close()
    }
)

export const state = derived<typeof ws, PlayerGameState>(ws, (ws, set, update) => {
    ws.addEventListener("message", (event) => {
        const message = JSON.parse(event.data) as Message
        switch (message.type) {
            case Message.Type.AcceptanceFromServer:
                return set(message.state)
            case Message.Type.ToClient:
                return set(message.state)
            case Message.Type.KeepAlive:
                return
            case Message.Type.AcceptanceFromClient:
            case Message.Type.Rejection:
            case Message.Type.ToServer:
            default:
                throw new Error(`unexpected message type - ${message.type}`)
        }
    })
})

interface Commander {
    joinGame: (playerId: PlayerId) => void
}

export const commander = derived<typeof ws, Commander>(ws, (ws, set, update) => {
    const doCommand = (command: PlayerCommand) => {
        const message: Message.ToServer = {
            command: command,
            id: uuidv4(),
            type: Message.Type.ToServer
        }

        const json = JSON.stringify(message)
        console.log(`sending command - ${json}`)
        ws.send(json)
    }

    set({
        joinGame: (playerId: PlayerId) => doCommand({actor: playerId, type: PlayerCommand.Type.JoinGame}),
    })
})
