import {DisconnectGameEventListener, EventType, listenToGameEvents} from "../GameEvents";
import {GamePhase} from "../Constants";

export class GamePhaseElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToGameEvents({
            [EventType.GameStarted]: () => {
                this.replaceChildren()
                this.innerHTML = `<h2 id="gamePhase"></h2>`
            },
            [EventType.RoundStarted]: () => this.updateGamePhase(GamePhase.Bidding, "Place your bid!"),
            [EventType.BiddingCompleted]: () => this.updateGamePhase(GamePhase.BiddingCompleted, "Bidding completed :)"),
            [EventType.TrickStarted]: () => this.updateGamePhase(GamePhase.TrickTaking, "It's trick taking time!"),
            [EventType.TrickCompleted]: () => this.updateGamePhase(GamePhase.TrickCompleted, "Trick completed :)"),
        })
    }

    updateGamePhase = (gamePhase: GamePhase, text: string) => {
        const gamePhaseEl = this.querySelector("#gamePhase") as HTMLHeadingElement
        gamePhaseEl.setAttribute("data-phase", gamePhase)
        gamePhaseEl.innerText = text
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}
