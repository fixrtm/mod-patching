use crossterm::event::{Event, KeyCode, KeyModifiers, MouseEventKind};
use crossterm::tty::IsTty;
use crossterm::*;
use fuzzy_matcher::FuzzyMatcher;

pub struct Selector<'a, 's, W: std::io::Write> {
    tty: W,
    items: &'a [&'s str],
    filtered: Vec<(i64, usize, &'s str)>,
    last_height: u16,
    candidate_limit: u16,
    scroll: usize,
    selecting: usize,
    inputting: String,
    matcher: fuzzy_matcher::skim::SkimMatcherV2,
}

// public apis
impl<'a, 's> Selector<'a, 's, std::io::Stdout> {
    pub fn new(items: &'a [&'s str]) -> Option<Self> {
        let stdout = std::io::stdout();
        if !stdout.is_tty() {
            return None;
        }
        Some(Selector::new_with(stdout, items))
    }
}

impl<'a, 's, W: std::io::Write> Selector<'a, 's, W> {
    pub fn new_with(tty: W, items: &'a [&'s str]) -> Self {
        Self {
            tty,
            items,
            filtered: vec![],
            last_height: 0,
            candidate_limit: u16::MAX,
            scroll: 0,
            selecting: 0,
            inputting: "".into(),
            matcher: fuzzy_matcher::skim::SkimMatcherV2::default().ignore_case(),
        }
    }

    pub fn with_candidate_limit(mut self, candidate_limit: u16) -> Self {
        self.candidate_limit = candidate_limit;
        self
    }

    pub fn interact(mut self) -> crossterm::Result<&'s str> {
        self.init()?;
        loop {
            if let Some(value) = self.handle_event(event::read()?)? {
                return Ok(value);
            }
        }
    }
}

// internals
impl<'a, 's, W: std::io::Write> Selector<'a, 's, W> {
    fn init(&mut self) -> crossterm::Result<()> {
        terminal::enable_raw_mode()?;
        self.filtered = self
            .items
            .iter()
            .enumerate()
            .map(|(i, x)| (0, i, *x))
            .collect();
        self.tty
            .queue(terminal::DisableLineWrap)?
            .queue(event::EnableMouseCapture)?;
        self.draw()?;
        Ok(())
    }

    fn handle_event(&mut self, event: event::Event) -> crossterm::Result<Option<&'s str>> {
        match event {
            Event::Key(e) => {
                if e.modifiers.contains(KeyModifiers::CONTROL) {
                    return Err(std::io::Error::new(
                        std::io::ErrorKind::ConnectionAborted,
                        "ctrl action",
                    ));
                } else {
                    if let Some(some) = self.handle_key_input(e.code)? {
                        return Ok(Some(some));
                    }
                }
            }
            Event::Mouse(event::MouseEvent { kind, row, .. }) => match kind {
                MouseEventKind::Down(event::MouseButton::Left) => {
                    if let Some(index) = self.row_to_index(row) {
                        self.selecting = index as usize + self.scroll;
                        self.draw()?;
                    }
                }
                MouseEventKind::ScrollDown => {
                    if let Some(_) = self.row_to_index(row) {
                        self.scroll_down(2);
                        self.draw()?;
                    }
                }
                MouseEventKind::ScrollUp => {
                    if let Some(_) = self.row_to_index(row) {
                        self.scroll_up(2);
                        self.draw()?;
                    }
                }
                _ => {}
            },
            Event::Resize(_, _) => self.draw()?,
        }
        Ok(None)
    }

    fn clear_list(&mut self) -> crossterm::Result<()> {
        for _ in 0..self.last_height {
            self.tty
                .queue(terminal::Clear(terminal::ClearType::CurrentLine))?
                .queue(cursor::MoveUp(1))?;
        }
        self.tty.queue(cursor::MoveToColumn(0))?;
        Ok(())
    }

    fn draw(&mut self) -> crossterm::Result<()> {
        self.clear_list()?;

        let (_, tty_height) = terminal::size()?;

        let list_height = (tty_height as usize - 1)
            .min(self.candidate_limit as usize)
            .min(self.filtered.len() - self.scroll) as u16;
        self.last_height = list_height;

        for (i, (_, _, line)) in self
            .filtered
            .iter()
            .enumerate()
            .skip(self.scroll)
            .take(list_height as usize)
        {
            self.tty
                .queue(terminal::Clear(terminal::ClearType::CurrentLine))?
                .queue(style::Print(if i == self.selecting { "> " } else { "  " }))?
                .queue(style::Print(line))?
                .queue(style::Print("\n"))?
                .queue(cursor::MoveToColumn(0))?;
        }
        self.tty
            .queue(terminal::Clear(terminal::ClearType::CurrentLine))?
            .queue(style::Print("search> "))?
            .queue(style::Print(&self.inputting))?
            .flush()?;
        Ok(())
    }

    fn fit_scroll(&mut self) {
        while self.selecting >= self.scroll + self.last_height as usize {
            self.scroll += 1;
        }
        while self.selecting < self.scroll {
            self.scroll -= 1;
        }
    }

    fn input_changed(&mut self) {
        self.filtered.clear();
        let matcher = &self.matcher;
        let inputting = &self.inputting;
        for v in self
            .items
            .iter()
            .enumerate()
            .flat_map(|(i, x)| matcher.fuzzy_match(x, inputting).map(|m| (m, i, *x)))
        {
            self.filtered.push(v)
        }
        self.filtered.sort_by_key(|x| std::cmp::Reverse(x.0));
        self.selecting = 0;
        self.scroll = 0;
    }

    pub(crate) fn handle_key_input(&mut self, key: KeyCode) -> crossterm::Result<Option<&'s str>> {
        match key {
            KeyCode::Enter => {
                if let Some((_, _, v)) = self.filtered.get(self.selecting) {
                    return Ok(Some(*v));
                }
            }
            KeyCode::Left => {}
            KeyCode::Right => {}
            KeyCode::Up => {
                if self.selecting > 0 {
                    self.selecting -= 1;
                    self.fit_scroll();
                    self.draw()?;
                }
            }
            KeyCode::Down => {
                if self.selecting < self.filtered.len() - 1 {
                    self.selecting += 1;
                    self.fit_scroll();
                    self.draw()?;
                }
            }
            KeyCode::Backspace => {
                self.inputting.pop();
                self.input_changed();
                self.draw()?;
            }
            KeyCode::Char(c) => {
                self.inputting.insert(self.inputting.len(), c);
                self.input_changed();
                self.draw()?;
            }
            _ => {}
        }
        Ok(None)
    }

    /// converts row in console to index on list.
    /// `self.row_to_index(row).map(|i| i + self.scroll)` returns index in `self.filtered`
    fn row_to_index(&mut self, row: u16) -> Option<u16> {
        let (_, cursor) = cursor::position().ok()?;
        let begin = cursor.checked_sub(self.last_height).unwrap_or_default();
        let end = cursor;

        if (begin..end).contains(&row) {
            Some(row - begin)
        } else {
            None
        }
    }

    fn scroll_down(&mut self, diff: usize) {
        self.scroll += diff;
        if self.scroll + self.last_height as usize >= self.filtered.len() {
            self.scroll = self.filtered.len() - self.last_height as usize;
        }
    }

    fn scroll_up(&mut self, diff: usize) {
        if self.scroll < diff {
            self.scroll = 0;
        } else {
            self.scroll -= diff;
        }
    }
}

impl<W: std::io::Write> Drop for Selector<'_, '_, W> {
    fn drop(&mut self) {
        self.clear_list().ok();
        self.tty.flush().ok();
        terminal::disable_raw_mode().ok();
        self.tty.execute(terminal::EnableLineWrap).ok();
        self.tty.execute(event::DisableMouseCapture).ok();
    }
}
