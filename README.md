
# Parenoia - My custom Clojure/script Editor

**ALPHA RELEASE**

## Paren + Fear = Parenoia

#### Unjustified fear of parens.

Edit your code through S-expressions. Dealing with text can be tedious, a lot of spaces, tabs, backspaces, just so your code looks better.

## Straight to the point?


Watch & compile with with hot reloading:

```
npm install
clj -X:dev
```

## Short tutorial

|buttons | action |
|--- | --- |
|`q, w, e, r` | slurping and barfing |
| `a, s, d`    | insert token before, in, after |
|`Shift + a, d`| insert token before, in, after but add a newline too.|
|`Ctrl+(Shift)+Z` | undo, redo as expected|
|`Enter` | edit selected token / s-expression|
|`Shift+Enter` | add newline (for breaking code in multiple lines)|
|`Backspace` | remove selected token / s-expression|
|`Shift+Backspace` | remove newline before selected token / s-expression|
|`Tab` | wrap with ()|
|`Shift+Tab` | unwrap|
|`m` | menu|
|`l` | Go to definition|
|`g` | namespace-graph|
|`;` | if editing token, when typing ";" autocomplete to all function names in project (also reg-event-db if you are a re-frame fan like me)|
|`Esc` | Close menu or finish editing token / s-expression|

## Why?

This editor is a tribute to Clojure/Script. A beautiful language deserves a beautiful editor.

## Why? but longer

1. Hate. I hate editing in a text editor. I have to imagine the structure in my head, also have to count parens when I want to cut out a block. Parinfer doesn't work well in all cases in VSCode. Paredit is not enough for me. 

2. Jealousy. Every profession has beautiful tools like AutoCad, Unreal Engine, Final Cut Pro, Figma ... All beautiful and practical apps.

3. Visual Cortex. I always felt this, but instead of me, maybe Jack Rusher will convince you: https://youtu.be/8Ab3ArE8W3s?t=1223

4. For fun. I like coding, I suppose I will like coding. Fun language needs fun tools, so worth a try.

## Inspiration 

This editor caught my eye: https://cirru.org/

Then found this video: https://www.youtube.com/watch?v=8Ab3ArE8W3s

And I decided that I'll make one of my dreams true, making programming visually pleasing, and more like a game.

## Features

  - Edit S-expressions 
  - Slurp and Barf
  - Global search in Clojure files (very slow)
  - Autocomplete for function names (re-frame keywords too)
  - Autoscroll on select
  - Pins. Like tabs in an editor, but brings you to the function instead of the file.
  - Namespaces instead of files, no folder structure.
  - clj-kondo lints intergrated (most of them)
  - Parinfer when editing s-expression
  - Namespace graph with vis. To see the dependencies visually.
  - clojure-lsp for token definitions, and references


## Plans for the future.

1. Connection to REPL somehow, ideally both Clojure and ClojureScript.
2. After REPL possibility to eval code like in calva.
3. (Plan for me) Make a webapp not compiled, but running REPL on the server and update only code. Only send new code to server, maybe skip Docker 🤞
4. Move-form. Seriously, how longer do we want to refactor by hand? Wake up, Clojure needs this (or just me, I am a lazy coder).
    - reference: https://github.com/clojure-lsp/clojure-lsp/issues/566
    - How I imagine it: 
      1. (user) Click rename
      2. (user) Instead of only function name, write namespace too: `new-namespace/my-function`
      3. (editor) Search the form for calls to other namespaces or the current one 
      4. (editor) Move the chain of functions to the new namespace. Why? Because there is instant circular dependency of you don't
      5. (editor) Add new requires in the new namespace
      6. (user) Profit
  
   - Why is it hard?
      1. Hard to think through all edge-cases
      2. I don't have every tool yet.
      3. Can cause circular dependency.
