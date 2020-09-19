package com.company;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RadixTree<C extends CharSequence>  {
    final List<String> data;
    static int passCount = 0;
    static StringBuilder stateBuilder = new StringBuilder();
    static final boolean ACCEPT = true;
    static final boolean REJECT = false;

    INodeCreator creator;
    Node root = null;

    public RadixTree() {
        this.creator = Node::new;
        data = new ArrayList<>();
    }

    public void add(C sequence) {
        this.addSequence(sequence);
        data.add((String) sequence);
    }

    void addSequence(C sequence) {
        if (root == null)
            root = this.creator.createNode(null, null, REJECT);

        int parentIndex = -1, stringIndex = -1;
        Node node = root;
        for (int i = 0; i <= sequence.length();) {
            stringIndex = i;
            parentIndex++;
            if (i == sequence.length())
                break;

            char c = sequence.charAt(i);
            if (node.isPart(c, parentIndex)) {
                i++;
                continue;
            } else if (node.string != null && parentIndex < node.string.length) {
                break;
            }

            Node child = node.getChildByChar(c);
            if (child != null) {
                parentIndex = 0;
                node = child;
                i++;
            } else {
                break;
            }
        }

        Node parent = node.parent;
        if (node.string != null && parentIndex < node.string.length) {
            char[] parentString = Arrays.copyOfRange(node.string, 0, parentIndex);
            char[] refactorString = Arrays.copyOfRange(node.string, parentIndex, node.string.length);

            if (parent != null)
                parent.removeChild(node);

            Node newParent = this.creator.createNode(parent, parentString, (stringIndex < sequence.length())? REJECT: ACCEPT);
            if (parent != null)
                parent.addChild(newParent);

            node.parent = newParent;
            node.string = refactorString;
            newParent.addChild(node);
            if (stringIndex < sequence.length()) {
                CharSequence newString = sequence.subSequence(stringIndex, sequence.length());
                Node newNode2 = this.creator.createNode(newParent, newString.toString().toCharArray(), ACCEPT);
                newParent.addChild(newNode2);
            }
        } else if (node.string != null && sequence.length() == stringIndex) {
            if (node.type == ACCEPT)
                return;

            node.type = ACCEPT;
        } else if (node.string != null) {
            CharSequence newString = sequence.subSequence(stringIndex, sequence.length());
            Node newNode = this.creator.createNode(node, newString.toString().toCharArray(), ACCEPT);
            node.addChild(newNode);
        } else {
            Node newNode = this.creator.createNode(node, sequence.toString().toCharArray(), ACCEPT);
            node.addChild(newNode);
        }
    }

    String getResult() {
        return getResult(this);
    }

    public void prepareResult() throws IOException {
        String result = getResult();
        List<String> lines = new ArrayList<>();
        lines.add(String.valueOf(passCount));
        lines.add(stateBuilder.toString());
        lines.add(result);
        
        Files.write(Paths.get("output.txt"), lines);
    }
    
    public void prepareDot() throws IOException {
        String[] lines = this.getResult().split("[\\r\\n]+");
        List<String> dotResult = new ArrayList<>();
        dotResult.add("digraph finite_state_machine {\n");

        StringBuilder dcStringBuilder = new StringBuilder();
        dcStringBuilder.append("\tnode [shape = doublecircle] ");
        for (String state: data)
            dcStringBuilder.append(state).append(" ");
        dotResult.add(dcStringBuilder.toString() + "\n\tnode [shape = circle];\n");

        for (String line: lines) {
            String[] items = line.split("\\s");
            if(items[0].isEmpty())
                continue;
            dotResult.add(items[0] + " -> " + items[2] + " [ label = \"" + items[1] + "\" ]");
        }

        dotResult.add("}");
        Files.write(Paths.get("data.dot"), dotResult);
    }

    static class Node implements Comparable<Node> {
        static final int MIN_SIZE = 2;
        Node[] children = new Node[MIN_SIZE];
        int childrenCount = 0;
        Node parent;
        boolean type = REJECT;
        char[] string = null;

        Node(Node parent) {
            this.parent = parent;
        }

        Node(Node parent, char[] sequence) {
            this(parent);
            this.string = sequence;
        }

        Node(Node parent, char[] sequence, boolean type) {
            this(parent, sequence);
            this.type = type;
        }

        void addChild(Node node) {
            int growSize = children.length;
            if (childrenCount >= children.length)
                children = Arrays.copyOf(children, (growSize + (growSize >> 1)));
            children[childrenCount++] = node;
            Arrays.sort(children, 0, childrenCount);
        }

        void removeChild(Node child) {
            if (childrenCount == 0) return;
            for (int i = 0; i < childrenCount; i++) {
                if (children[i].equals(child)) {
                    removeChild(i);
                    return;
                }
            }
        }

        void removeChild(int index) {
            if (index >= childrenCount)
                return;

            children[index] = null;
            childrenCount--;

            System.arraycopy(children, index + 1, children, index, childrenCount - index);
            int shrinkSize = childrenCount;
            if (childrenCount >= MIN_SIZE && childrenCount < (shrinkSize + (shrinkSize<<1)))
                System.arraycopy(children, 0, children, 0, childrenCount);
        }

        Node getChildByIndex(int index) {
            if (index >= childrenCount)
                return null;
            return children[index];
        }

        int getChildrenCount() {
            return childrenCount;
        }

        boolean isPart(char c, int idx) {
            return string != null && idx < string.length && string[idx] == c;
        }

        Node getChildByChar(char c) {
            for (int i = 0; i < this.childrenCount; i++) {
                Node child = this.children[i];
                if (child.string.length>0 && child.string[0] == c)
                    return child;
            }
            return null;
        }

        @Override
        public int compareTo(Node node) {
            if (node == null)
                return -1;

            int length = string.length;
            if (node.string.length < length) length = node.string.length;
            for (int i = 0; i < length; i++) {
                Character a = string[i];
                Character b = node.string[i];
                int c = a.compareTo(b);
                if (c != 0)
                    return c;
            }

            if (this.type == REJECT && node.type == ACCEPT)
                return -1;
            else if (node.type == REJECT && this.type == ACCEPT)
                return 1;

            if (this.getChildrenCount() < node.getChildrenCount())
                return -1;
            else if (this.getChildrenCount() > node.getChildrenCount())
                return 1;

            return 0;
        }
    }

    interface INodeCreator {
        Node createNode(Node parent, char[] sequence, boolean type);
    }

    static <C extends CharSequence> String getResult(RadixTree<C> tree) {
        if (tree.root == null)
            return "";

        return getResult(tree.root, null);
    }

    static String getResult(Node node, String previousString) {
        StringBuilder resultBuilder = new StringBuilder();
        String thisString = "";
        if (node.string != null)
            thisString = String.valueOf(node.string);
        String fullString = ((previousString != null) ? previousString : "") + thisString;

        if(!thisString.isEmpty()) {
            passCount++;
            stateBuilder.append(((node.type == ACCEPT) ? "1 " : "0 "));
            resultBuilder
                    .append(previousString)
                    .append(" ")
                    .append(thisString)
                    .append(" ")
                    .append((node.type == ACCEPT) ? fullString : previousString + thisString)
                    .append("\n");
        }

        if (node.children != null) {
            for (int i = 0; i < node.getChildrenCount() - 1; i++) {
                resultBuilder.append(getResult(node.getChildByIndex(i), fullString));
            }
            if (node.getChildrenCount() >= 1) {
                resultBuilder.append(getResult(node.getChildByIndex(node.getChildrenCount() - 1), fullString));
            }
        }

        return resultBuilder.toString();
    }
}