import {EventType, listenToGameEvents} from "../GameEvents";
import {GameState} from "../Constants";

export class GameStateElement extends HTMLElement {
    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToGameEvents({
            [EventType.PlayerJoined]: ({waitingForMorePlayers}) => this.updateBasedOnPlayers(waitingForMorePlayers),
            [EventType.GameStarted]: () => this.updateGameState(GameState.InProgress),
            [EventType.GameCompleted]: () => this.updateGameState(GameState.Complete),
        })

        this.innerHTML = `<h2 id="gameState"></h2>`
        this.updateBasedOnPlayers(INITIAL_STATE.waitingForMorePlayers)
    }

    updateBasedOnPlayers = (waitingForMorePlayers) => {
        if (waitingForMorePlayers) this.updateGameState(GameState.WaitingForMorePlayers)
        else this.updateGameState(GameState.WaitingToStart)
    }

    updateGameState = (gameState) => {
        const gameStateEl = this.querySelector("#gameState")
        const gameStateMapping = {
            [GameState.WaitingForMorePlayers]: "Waiting for more players...",
            [GameState.WaitingToStart]: "Waiting for the game to start",
            [GameState.InProgress]: "The game has started!",
            [GameState.Complete]: "The game is over!"
        }

        const text = gameStateMapping[gameState]
        if (!text) throw new Error("Unknown game state: " + gameState)
        gameStateEl.innerText = gameStateMapping[gameState]
        gameStateEl.setAttribute("data-state", gameState)
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}
