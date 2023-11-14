FROM      node:slim
COPY      prod                 /root/prod
COPY      node_modules         /root/prod/node_modules
WORKDIR   /root/prod
CMD       ["node", "core.js"]