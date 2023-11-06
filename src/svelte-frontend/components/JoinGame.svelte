<script lang="ts">
    import {CommandType, messageStore} from "../socket";

    let playerIdInput = ""
</script>

<form id="joinGame" on:submit|preventDefault={() => messageStore.send({type: CommandType.JoinGame, actor: playerIdInput})}>
    <label>
        Player ID:
        <input bind:value={playerIdInput} name="playerId" placeholder="Enter a name"/>
    </label>
    <input type="submit" value="Submit"/>
</form>
