function connectToWs(socket) {
    const EventType = {
        PlayerJoined: "GameEvent$PlayerJoined",
        GameStarted: "GameEvent$GameStarted",
        RoundStarted: "GameEvent$RoundStarted",
        BidPlaced: "GameEvent$BidPlaced",
        BiddingCompleted: "GameEvent$BiddingCompleted",
        CardPlayed: "GameEvent$CardPlayed",
        TrickCompleted: "GameEvent$TrickCompleted",
        TrickStarted: "GameEvent$TrickStarted",
        GameCompleted: "GameEvent$GameCompleted",
    }
    const knownEventTypes = Object.values(EventType)

    const GameState = {
        WaitingForMorePlayers: "WaitingForMorePlayers",
        WaitingToStart: "WaitingToStart",
        InProgress: "InProgress",
        Complete: "Complete",
    }

    const GamePhase = {
        Bidding: "Bidding",
        BiddingCompleted: "BiddingCompleted",
        TrickTaking: "TrickTaking",
        TrickComplete: "TrickComplete",
    }

    function listenToGameEvents(gameEventsToCallbacks) {
        const listener = (event) => {
            try {
                const data = JSON.parse(event.data)
                const callback = gameEventsToCallbacks[data.type]
                if (callback) return callback(data)

                if (!knownEventTypes.includes(data.type)) {
                    socket.send(JSON.stringify({
                        type: "ClientMessage$UnhandledMessageFromServer",
                        offender: data.type,
                    }))
                    console.error(`Unknown message from server: ${data.type}`)
                }
            } catch (e) {
                socket.send(JSON.stringify({
                    stackTrace: e.stack,
                    type: "ClientMessage$Error",
                }))
                throw e
            }
        }
        socket.addEventListener("message", listener)
        return () => socket.removeEventListener("message", listener)
    }

    configureWs()

    function configureWs() {
        const body = document.querySelector("body")
        const biddingForm = document.createElement("sk-biddingform")
        body.appendChild(biddingForm)

        const bidsEl = document.createElement("sk-bids")
        body.appendChild(bidsEl)

        const hand = document.createElement("sk-hand")
        body.appendChild(hand)

        function updateGameState(gameState) {
            const gameStateEl = document.getElementById("gameState")
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

        function updateGamePhase(gamePhase) {
            const gamePhaseEl = document.getElementById("gamePhase")
            const gamePhaseMapping = {
                [GamePhase.Bidding]: "Place your bid!",
                [GamePhase.BiddingCompleted]: "Bidding completed :)",
                [GamePhase.TrickTaking]: "It's trick taking time!",
                [GamePhase.TrickComplete]: "Trick completed :)"
            }

            const text = gamePhaseMapping[gamePhase]
            if (!text) throw new Error("Unknown game phase: " + gamePhase)
            gamePhaseEl.innerText = gamePhaseMapping[gamePhase]
            gamePhaseEl.setAttribute("data-phase", gamePhase)
        }

        socket.addEventListener("close", (event) => {
            console.error("disconnected from ws")
        })

        socket.addEventListener("message", (event) => {
            try {
                const data = JSON.parse(event.data)
                switch (data.type) {
                    case EventType.PlayerJoined:
                        const players = document.getElementById("players")
                        const li = document.createElement("li")
                        li.innerText = data.playerId
                        players.appendChild(li)

                        if (data.waitingForMorePlayers) updateGameState(GameState.WaitingForMorePlayers)
                        else updateGameState(GameState.WaitingToStart)

                        return
                    case EventType.GameStarted:
                        updateGameState(GameState.InProgress)
                        return
                    case EventType.RoundStarted:
                        const roundNumberEl = document.getElementById("roundNumber")
                        roundNumberEl.innerText = data.roundNumber
                        updateGamePhase("Bidding")

                        const trickNumberEl = document.getElementById("trickNumber")
                        trickNumberEl.innerText = ""

                        const trick = document.getElementById("trick")
                        trick.textContent = ""
                        return
                    case EventType.BidPlaced:
                        return
                    case EventType.BiddingCompleted:
                        updateGamePhase("BiddingCompleted")
                        return
                    case EventType.CardPlayed:
                        const trick1 = document.getElementById("trick")
                        const li1 = document.createElement("li")
                        li1.innerText = `${data.playerId}:${data.card.name}`
                        li1.setAttribute("player", data.playerId)
                        li1.setAttribute("suit", data.card.suit)
                        data.card.number && li1.setAttribute("number", data.card.number)
                        return trick1.appendChild(li1);
                    case EventType.TrickCompleted:
                        return updateGamePhase("TrickComplete");
                    case EventType.TrickStarted:
                        updateGamePhase("TrickTaking")
                        const trickNumberEl1 = document.getElementById("trickNumber")
                        trickNumberEl1.innerText = data.trickNumber

                        const trick2 = document.getElementById("trick")
                        return trick2.textContent = "";
                    case EventType.GameCompleted:
                        updateGameState(GameState.Complete)
                        return
                    default: {
                        socket.send(JSON.stringify({
                            type: "ClientMessage$UnhandledMessageFromServer",
                            offender: data.type,
                        }))
                        console.error(`Unknown message from server: ${data.type}`)
                    }
                }
            } catch (e) {
                socket.send(JSON.stringify({
                    stackTrace: e.stack,
                    type: "ClientMessage$Error",
                }))
                throw e
            }
        });
    }

    class BiddingForm extends HTMLElement {
        constructor() {
            super()
        }

        connectedCallback() {
            this.disconnectedCallback()
            this.disconnectFn = listenToGameEvents({
                [EventType.RoundStarted]: this.showForm,
                [EventType.BiddingCompleted]: this.hideForm,
            })
        }

        showForm = ({roundNumber}) => {
            this.innerHTML = `
                <label>Bid <input type="number" name="bid" min="0" max="${roundNumber}"></label>
                <button id="placeBid" type="button" onclick="onBidSubmit()" disabled>Place Bid</button>
                <p id="biddingError"></p>
            `

            const placeBidBtn = this.querySelector("#placeBid");
            const bidInput = this.querySelector(`input[name="bid"]`);
            const biddingError = this.querySelector("#biddingError");

            bidInput.oninput = (e) => {
                const bid = e.target.value.replace(/[^0-9]/g, '')
                bidInput.value = bid
                if (bid >= 0 && bid <= roundNumber) {
                    placeBidBtn.disabled = false
                    biddingError.innerText = ""
                    return
                }

                placeBidBtn.disabled = true
                biddingError.innerText = `Bid must be between 0 and ${roundNumber}`
            }

            placeBidBtn.onclick = () => {
                socket.send(JSON.stringify({
                    type: "ClientMessage$BidPlaced",
                    bid: bidInput.value,
                }))
                this.hideForm()
            }
        }

        hideForm = () => this.replaceChildren()

        disconnectedCallback() {
            if (this.disconnectFn) this.disconnectFn()
            this.disconnectFn = undefined
        }
    }

    class Bids extends HTMLElement {
        constructor() {
            super();
            listenToGameEvents({
                [EventType.GameStarted]: ({players}) => this.initialiseForPlayers(players),
                [EventType.BidPlaced]: ({playerId}) => this.indicateThatPlayerHasBid(playerId),
                [EventType.BiddingCompleted]: ({bids}) => this.showActualBids(bids),
            })
        }

        connectedCallback() {
            this.innerHTML = `
                <section id="bidsArea">
                    <h3>Bids</h3>
                    <ul id="bids"></ul>
                </section>
            `
        }

        initialiseForPlayers = (players) => {
            const bids = this.querySelector("#bids")
            players.forEach(playerId => {
                const li = document.createElement("li")
                li.textContent = playerId
                li.setAttribute("data-playerBid", playerId)
                li.appendChild(document.createElement("span"))
                bids.appendChild(li)
            })
        }

        indicateThatPlayerHasBid = (playerId) => {
            this.querySelector(`[data-playerBid="${playerId}"] span`).innerText = ":" + "has bid"
        }

        showActualBids = (bids) => {
            this.querySelectorAll(`[data-playerBid]`).forEach(el => {
                const playerId = el.getAttribute("data-playerBid")
                const bid = bids[playerId]
                el.querySelector("span").innerText = ":" + bid
            })
        }
    }

    class Hand extends HTMLElement {
        constructor() {
            super()
        }

        connectedCallback() {
            this.disconnectedCallback()
            this.disconnectFn = listenToGameEvents({
                [EventType.GameStarted]: this.showHand,
                [EventType.RoundStarted]: ({ cardsDealt }) => this.initialiseHand(cardsDealt),
            })
        }

        showHand = () => {
            this.innerHTML = `
                <section id="handArea">
                    <h3>Hand</h3>
                    <ul id="hand"></ul>
                </section> 
            `
        }

        initialiseHand = (cards) => {
            const hand = this.querySelector("#hand")
            cards.forEach(card => {
                const li = document.createElement("li")
                li.innerText = card.name
                li.setAttribute("suit", card.suit)
                card.number && li.setAttribute("number", card.number)

                const button = document.createElement("button")
                button.innerText = "Play"
                button.onclick = function playCard() {
                    li.remove()
                    socket.send(JSON.stringify({
                        type: "ClientMessage$CardPlayed",
                        cardName: card.name,
                    }))
                }
                li.appendChild(button)
                hand.appendChild(li)
            })
        }

        disconnectedCallback() {
            if (this.disconnectFn) this.disconnectFn()
            this.disconnectFn = undefined
        }
    }

    customElements.define("sk-biddingform", BiddingForm)
    customElements.define("sk-bids", Bids)
    customElements.define("sk-hand", Hand)
}
