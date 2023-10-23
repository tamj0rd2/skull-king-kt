import {NotificationType, listenToNotifications, CommandType} from "../GameEvents";
import {Card} from "../Constants";
import {DisconnectGameEventListener, sendCommand} from "../Socket";

export class HandElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToNotifications({
            [NotificationType.GameStarted]: this.showHand,
            [NotificationType.RoundStarted]: ({ cardsDealt }) => this.initialiseHand(cardsDealt),
            [NotificationType.YourTurn]: ({ cards }) => this.makeCardsPlayable(cards),
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

    initialiseHand = (cards: Card[]) => {
        const hand = this.querySelector("#hand")!!
        cards.forEach(card => {
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
            button.parentNode!!.removeChild(button)
        })
    }

    makeCardsPlayable = (cardNamesToPlayability: Record<string, boolean> ) => {
        this.querySelectorAll("li").forEach(li => {
            const cardName = li.getAttribute("data-cardName")!!
            const isPlayable = cardNamesToPlayability[cardName]
            if (!isPlayable) return

            const button = document.createElement("button")
            button.innerText = "Play"
            button.onclick = async () => {
                li.remove()
                this.makeCardsUnplayable()
                await sendCommand({
                    type: CommandType.PlayCard,
                    cardName: cardName,
                })
            }
            li.appendChild(button)
        })
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-hand", HandElement)
