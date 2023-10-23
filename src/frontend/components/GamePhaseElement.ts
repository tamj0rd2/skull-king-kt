import {NotificationType, listenToNotifications} from "../GameEvents";
import {GamePhase} from "../Constants";
import {DisconnectGameEventListener} from "../Socket";

export class GamePhaseElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToNotifications({
            [NotificationType.GameStarted]: () => {
                this.replaceChildren()
                this.innerHTML = `<h2 id="roundPhase"></h2>`
            },
            [NotificationType.RoundStarted]: () => this.updateGamePhase(GamePhase.Bidding, "Place your bid"),
            [NotificationType.BiddingCompleted]: () => this.updateGamePhase(GamePhase.BiddingCompleted, "All bids are in"),
            [NotificationType.TrickStarted]: () => this.updateGamePhase(GamePhase.TrickTaking, ""),
            [NotificationType.TrickCompleted]: () => this.updateGamePhase(GamePhase.TrickCompleted, ""),
        })
    }

    updateGamePhase = (gamePhase: GamePhase, text: string) => {
        const gamePhaseEl = this.querySelector("#roundPhase") as HTMLHeadingElement
        gamePhaseEl.setAttribute("data-phase", gamePhase)
        gamePhaseEl.innerText = text
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-gamephase", GamePhaseElement)
