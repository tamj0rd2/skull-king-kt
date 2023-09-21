import {DisconnectGameEventListener, MessageToClient, listenToGameEvents} from "../GameEvents";
import {PlayerId} from "../Constants";

export class PlayersElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener
    playersElement?: HTMLUListElement

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToGameEvents({
            [MessageToClient.PlayerJoined]: ({ playerId }) => this.addPlayer(playerId),
            [MessageToClient.GameStarted]: () => this.parentNode!!.removeChild(this),
        })

        this.innerHTML = `
                <h3>Players</h3>
                <ul id="players"></ul>
            `

        this.playersElement = document.querySelector("#players") as HTMLUListElement
        INITIAL_STATE.players.forEach(this.addPlayer)
    }

    addPlayer = (playerId: PlayerId) => {
        const li = document.createElement("li")
        li.innerText = playerId
        this.playersElement!!.appendChild(li)
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-players", PlayersElement)
