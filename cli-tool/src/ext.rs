use std::io::{BufReader, BufWriter, Read, Write};

pub trait ReadExt: Sized {
    fn buf_read(self) -> BufReader<Self>;
}

impl<T: Read> ReadExt for T {
    fn buf_read(self) -> BufReader<Self> {
        BufReader::new(self)
    }
}

pub trait WriteExt: Sized + Write {
    fn buf_write(self) -> BufWriter<Self>;
}

impl<T: Write> WriteExt for T {
    fn buf_write(self) -> BufWriter<Self> {
        BufWriter::new(self)
    }
}
