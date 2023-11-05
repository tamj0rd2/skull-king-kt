<script lang="ts">
    import {CommandType, waitingForServerResponse, messageStore, playerId} from "./socket";
    import {bids, BidState, type DisplayBid, gameState, players, roundNumber, roundPhase} from "./GameState";
    import {RoundPhase, GameState, type PlayerId} from "./constants";
    import Spinner from "./components/Spinner.svelte";

    let playerIdInput = ""
    let bidInput = 0

    function joinGame(e: Event) {
        e.preventDefault()
        e.stopImmediatePropagation()
        messageStore.send({type: CommandType.JoinGame, actor: playerIdInput})
    }

    function placeBid(e: Event) {
        e.preventDefault()
        e.stopImmediatePropagation()
        messageStore.send({type: CommandType.PlaceBid, bid: bidInput, actor: $playerId})
    }

    function gameStateAsText(gameState: GameState): string {
        switch (gameState) {
            case GameState.InProgress:
                return ""
            case GameState.Complete:
                return "Game over!"
            case GameState.WaitingForMorePlayers:
                return "Waiting for more players..."
            case GameState.WaitingToStart:
                return "Waiting to start..."
        }
    }

    function roundPhaseAsText(roundPhase: RoundPhase): string {
        switch (roundPhase) {
            case RoundPhase.Bidding:
                return "Place your bid"
            case RoundPhase.BiddingCompleted:
                return "All bids are in!"
            case RoundPhase.TrickTaking:
                return ""
            case RoundPhase.TrickCompleted:
                return ""
        }
    }

    function headingText(playerId: PlayerId) {
        let text = "Game Page"
        if (!!playerId) {
            text += ` - ${playerId}`
        }
        return text
    }

    function bidText(playerId: PlayerId, displayBid: DisplayBid): string {
        switch (displayBid.bidState) {
            case BidState.None:
                return `${playerId}`
            case BidState.Hidden:
                return `${playerId}:has bid`
            case BidState.Placed:
                return `${playerId}:${displayBid.bid}`
        }
    }
</script>

{#if $waitingForServerResponse}
    <Spinner/>
{/if}

<h1>{headingText($playerId)}</h1>

{#if !$playerId}
    <form id="joinGame" on:submit={joinGame}>
        <label>
            Player ID:
            <input bind:value={playerIdInput} name="playerId" placeholder="Enter a name"/>
        </label>
        <input type="submit" value="Submit"/>
    </form>
{:else}
    <h2 id="gameState" data-state={$gameState}>{gameStateAsText($gameState)}</h2>
    <h3>Players:</h3>
    <ul id="players">
        {#each $players as player}
            <li>{player}</li>
        {/each}
    </ul>

    {#if $roundPhase}
        <h2 id="roundPhase" data-phase={$roundPhase}>{roundPhaseAsText($roundPhase)}</h2>
        {#if $roundPhase === RoundPhase.Bidding}
            <form id="placeBid" on:submit={placeBid}>
                <label>Bid <input bind:value={bidInput} type="number" name="bid" min="0" max="{$roundNumber}"></label>
                <input type="submit" value="Place Bid"/>
                <p id="biddingError"></p>
            </form>
        {/if}
        <ul id="bids">
            {#each Object.entries($bids) as [playerId, displayBid]}
                <li>{bidText(playerId, displayBid)}</li>
            {/each}
        </ul>
    {/if}
{/if}

<h4>Messages for debugging:</h4>
<ul>
    {#each $messageStore as message}
        <li class="message">
            <pre>{JSON.stringify(message)}</pre>
        </li>
    {/each}
</ul>

<style>
    .message pre {
        white-space: break-spaces;
    }
</style>
