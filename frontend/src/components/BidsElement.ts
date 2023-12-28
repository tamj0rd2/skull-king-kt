import {DisconnectGameEventListener, listenToNotifications} from "../Socket";
import {Notification, PlayerId} from "../../generated_types";

export class BidsElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super();
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToNotifications({
            [Notification.Type.GameStarted]: ({players}) => this.initialiseForPlayers(players),
            [Notification.Type.BidPlaced]: ({playerId}) => this.indicateThatPlayerHasBid(playerId),
            [Notification.Type.BiddingCompleted]: ({bids}) => this.showActualBids(bids),
        })
    }

    initialiseForPlayers = (players: PlayerId[]) => {
        this.innerHTML = `
                <section id="bidsArea">
                    <h3>Bids</h3>
                    <ul id="bids"></ul>
                </section>
            `

        const bids = this.querySelector("#bids")!!
        players.forEach(playerId => {
            const li = document.createElement("li")
            li.textContent = playerId
            li.setAttribute("data-playerBid", playerId)
            li.appendChild(document.createElement("span"))
            bids.appendChild(li)
        })
    }

    indicateThatPlayerHasBid = (playerId: PlayerId) => {
        const span: HTMLSpanElement = this.querySelector(`[data-playerBid="${playerId}"] span`)!!
        span.innerText = ":" + "has bid"
    }

    showActualBids = (bids: {[playerId: PlayerId]: number}) => {
        this.querySelectorAll(`[data-playerBid]`).forEach(el => {
            const playerId = el.getAttribute("data-playerBid") as PlayerId
            const bid = bids[playerId]
            const span = el.querySelector("span")!!
            span.innerText = ":" + bid
        })
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-bids", BidsElement)