import Mocha = require('mocha');
import * as path from 'path';

const mocha = new Mocha({ ui: 'bdd', color: true });
mocha.addFile(path.join(__dirname, 'unit', 'csvCodec.test.js'));

mocha.run((failures: number) => {
  process.exit(failures > 0 ? 1 : 0);
});
