import {EventType, listenToGameEvents} from "../GameEvents";
import {GameState} from "../Constants";

export class PlayersElement extends HTMLElement {
    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToGameEvents({
            [EventType.PlayerJoined]: ({ playerId }) => this.addPlayer(playerId),
            [EventType.GameStarted]: () => this.parentNode.removeChild(this),
        })

        this.innerHTML = `
                <h3>Players</h3>
                <ul id="players"></ul>
            `

        this.playersElement = document.querySelector("#players")
        INITIAL_STATE.players.forEach(this.addPlayer)
    }

    addPlayer = (playerId) => {
        const li = document.createElement("li")
        li.innerText = playerId
        this.playersElement.appendChild(li)
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}
