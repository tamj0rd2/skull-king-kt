import {Card, CardWithPlayability, CommandType, NotificationType} from "../Constants";
import {DisconnectGameEventListener, getPlayerId, listenToNotifications, sendCommand} from "../Socket";

export class HandElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToNotifications({
            [NotificationType.GameStarted]: this.showHand,
            [NotificationType.RoundStarted]: ({cardsDealt}) => this.initialiseHand(cardsDealt),
            [NotificationType.YourTurn]: ({cards}) => this.makeCardsPlayable(cards),
            [NotificationType.TrickCompleted]: () => this.makeCardsUnplayable(),
        })
    }

    showHand = () => {
        this.replaceChildren()
        this.innerHTML = `
            <section id="handArea">
                <h3>Hand</h3>
                <ul id="hand"></ul>
            </section> 
        `
    }

    initialiseHand = (cards: CardWithPlayability[]) => {
        const hand = this.querySelector("#hand")!!
        cards.forEach(({card}) => {
            const li = document.createElement("li")
            li.innerText = card.name
            li.setAttribute("data-cardType", card.type)
            li.setAttribute("data-cardName", card.name)
            li.setAttribute("suit", card.suit)
            card.number && li.setAttribute("number", card.number.toString())
            hand.appendChild(li)
        })
    }

    makeCardsUnplayable = () => {
        this.querySelectorAll("li button").forEach(button => {
            button.remove()
        })
    }

    makeCardsPlayable = (cards: CardWithPlayability[]) => {
        cards.forEach(({card, isPlayable}) => {
            if (!isPlayable) return

            this.querySelectorAll(`li[data-cardName="${card.name}"]`).forEach((li) => {
                const button = document.createElement("button")
                button.innerText = "Play"
                button.onclick = () => {
                    li.remove()
                    this.makeCardsUnplayable()
                    sendCommand({
                        type: CommandType.PlayCard,
                        cardName: card.name,
                        actor: getPlayerId(),
                    }).catch((reason) => {
                        throw reason
                    })
                }
                li.appendChild(button)
            })
        })
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-hand", HandElement)
