#!/bin/bash

echo $(zipinfo -1 $1|grep "\.dex"|wc -l)
