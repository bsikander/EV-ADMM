# EV-ADMM - Decentralized master aggregation branch
This branch contains the code to test the the EVADMM by reducing the network communication by doing distributed MEAN. Currently, in reduced communication branch only xSUM is being sent to the master and all other code has been commented out. Further, all the convergence code has been commented out because it is not being used currently.

This enchancement includes reducing the network communication to almost zero. This can be achieved by averaging the optimal values on each machine and sending just the optimal answer.
