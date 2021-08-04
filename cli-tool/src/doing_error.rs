use quick_error::quick_error;

quick_error! {
    #[derive(Debug)]
    pub enum DoingError {
        DoingStr(place: &'static str, err: Box<dyn std::error::Error + 'static>) {
            display("{}: {}", place, err)
            source(err.as_ref())
        }
        DoingString(place: String, err: Box<dyn std::error::Error + 'static>) {
            display("{}: {}", place, err)
            source(err.as_ref())
        }
    }
}

pub trait DoingStr<R>: Sized {
    fn doing(self, msg: &'static str) -> R;
}

pub trait DoingStr1<R>: Sized {
    fn doing(self, msg: &'static str) -> R;
}

pub trait DoingString<R>: Sized {
    fn doing_string(self, msg: String) -> R;
}

impl<T: std::error::Error + 'static> DoingString<DoingError> for T {
    fn doing_string(self, msg: String) -> DoingError {
        DoingError::DoingString(msg, Box::new(self))
    }
}

impl<V, T: std::error::Error + 'static> DoingStr<Result<V, DoingError>> for Result<V, T> {
    fn doing(self, msg: &'static str) -> Result<V, DoingError> {
        self.map_err(|x| DoingError::DoingStr(msg, Box::new(x)))
    }
}

impl<V> DoingStr1<Result<V, DoingError>> for Result<V, Box<dyn std::error::Error>> {
    fn doing(self, msg: &'static str) -> Result<V, DoingError> {
        self.map_err(|x| DoingError::DoingStr(msg, x))
    }
}

impl<T: std::error::Error + 'static> DoingStr<DoingError> for T {
    fn doing(self, msg: &'static str) -> DoingError {
        DoingError::DoingStr(msg, Box::new(self))
    }
}
