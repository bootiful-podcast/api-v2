#!/usr/bin/env zsh

id=$( kubectl get pods | grep api | cut -f1 -d\ )
kubectl port-forward $id 8080 