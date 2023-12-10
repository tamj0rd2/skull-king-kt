<script>
    import {messageStore} from "../socket";
</script>

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
