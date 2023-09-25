import {DisconnectGameEventListener, MessageToClient, listenToGameEvents} from "../GameEvents";
import {ActualBids, PlayerId, PlayerIds} from "../Constants";

export class WinsElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super();
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToGameEvents({
            [MessageToClient.RoundStarted]: () => this.replaceChildren(),
            [MessageToClient.RoundCompleted]: ({wins}) => this.showWins(wins),
        })
    }

    showWins = (wins: Record<PlayerId, number>) => {
        this.innerHTML = `
            <section id="winsArea">
                <h3>Wins</h3>
                <ul id="wins"></ul>
            </section>
        `

        const winsEl = this.querySelector("#wins")!!
        Object.entries(wins).forEach(([playerId, wins]) => {
            const li = document.createElement("li")
            li.textContent = `${playerId} won ${wins}`
            li.setAttribute("data-playerId", playerId)
            li.setAttribute("data-wins", wins.toString())
            winsEl.appendChild(li)
        })
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-wins", WinsElement)
