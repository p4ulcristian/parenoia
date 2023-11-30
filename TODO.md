Rewrite-clj barf-forward doesnt work

https://github.com/clj-commons/rewrite-clj/issues/245


Also later need to do swap 
  - swap to left
  - swap to right

  (let [first-removed (z/remove current-zloc)
                    second-removed (z/remove first-removed)
                    leftmost-removed? (z/leftmost? first-removed)
                    after-removed (if leftmost-removed? (z/down second-removed) second-removed)
                    first-value   (if leftmost-removed? 
                                    (z/insert-left after-removed value-before)
                                    (z/insert-right after-removed value-before))
                    second-value  (z/insert-right first-value value)]                  
                  (z/right second-value))


Something like that.