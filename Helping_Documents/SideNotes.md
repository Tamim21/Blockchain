# Simple Blockchain Assignment

add block

```python-repl
int currentHeight = tree.maxHeightNode.height;
        int maxHeight = currentHeight - CUT_OFF_AGE;

        if (block.getPrevBlockHash() == null) {
            return false; // Genesis block cannot be added
        }

        if (block.getPrevBlockHash().equals(tree.maxHeightNode.block.getHash())) {
            Tree.Node newNode = new Tree.Node(block, tree.maxHeightNode);
            tree.maxHeightNode.children.add(newNode);
            newNode.height = currentHeight + 1;
            tree.maxHeightNode = newNode;
            return true;
        }

        if (block.getPrevBlockHash().equals(tree.maxHeightNode.parent.block.getHash())) {
            Tree.Node parentNode = tree.maxHeightNode.parent;
            Tree.Node newNode = new Tree.Node(block, parentNode);
            parentNode.children.add(newNode);
            newNode.height = currentHeight + 1;
            tree.maxHeightNode = newNode;
            return true;
        }

        if (block.getPrevBlockHash().equals(tree.maxHeightNode.parent.parent.block.getHash())) {
            Tree.Node grandparentNode = tree.maxHeightNode.parent.parent;
            Tree.Node parentNode = tree.maxHeightNode.parent;
            Tree.Node newNode = new Tree.Node(block, grandparentNode);
            grandparentNode.children.remove(parentNode);
            grandparentNode.children.add(newNode);
            newNode.height = currentHeight + 1;
            tree.maxHeightNode = newNode;
            return true;
        }

        return false;
```
