# Skull king

- [Live](https://skull-king-kt.fly.dev/play)

## Deployment

- [Install flyctl](https://fly.io/docs/hands-on/install-flyctl/)
- `flyctl auth login`
- `flyctl deploy`

### Other helpful commands

- Suspend the app using - `flyctl scale count 0`

## Development

### Setting up ChromeDriver

**These steps only work for Macs with arm chips**

- Install Chrome and ChromeDriver
  ```shell
    ./setup-chrome.sh
  ```
- Update the chrome version in the WebTests file

### Arrow optics

Certain folders need to be marked as generated source routes for the IDE to know
about them. https://kotlinlang.org/docs/ksp-quickstart.html#make-ide-aware-of-generated-code
