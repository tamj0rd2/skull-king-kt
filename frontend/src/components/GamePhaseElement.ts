import {DisconnectGameEventListener, listenToNotifications} from "../Socket";
import {Notification, RoundPhase} from "../../generated_types";

export class GamePhaseElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToNotifications({
            [Notification.Type.GameStarted]: () => {
                this.replaceChildren()
                this.innerHTML = `<h2 id="roundPhase"></h2>`
            },
            [Notification.Type.RoundStarted]: () => this.updateGamePhase(RoundPhase.Bidding, "Place your bid"),
            [Notification.Type.BiddingCompleted]: () => this.updateGamePhase(RoundPhase.BiddingCompleted, "All bids are in"),
            [Notification.Type.TrickStarted]: () => this.updateGamePhase(RoundPhase.TrickTaking, ""),
            [Notification.Type.TrickCompleted]: () => this.updateGamePhase(RoundPhase.TrickCompleted, ""),
        })
    }

    updateGamePhase = (roundPhase: RoundPhase, text: string) => {
        const gamePhaseEl = this.querySelector("#roundPhase") as HTMLHeadingElement
        gamePhaseEl.setAttribute("data-phase", roundPhase)
        gamePhaseEl.innerText = text
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-gamephase", GamePhaseElement)
