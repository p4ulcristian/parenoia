
Node.js example for [shadow-cljs](https://github.com/thheller/shadow-cljs)
----

### Develop

Watch & compile with with hot reloading:

```
npm install
clj -X:dev
```


### Connect to VSCode Calva

1. `CMD + SHIFT + P`
2. Type `Connecting to a running REPL server in the ...`
3. Choose `shadow-cljs`
4. Use automatic port, or copy from terminal output after running `clj -X:dev`
5. Choose `:node-repl`
6. `Ctrl + Enter` to run function inline.
7. Enjoy.


### Build

```
clj -X:prod
```
